package PSM.Location.api.stop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import PSM.Location.Stop;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class GeoJsonGeneratorService {
    private static final Logger logger = LoggerFactory.getLogger(GeoJsonGeneratorService.class);
    private static final String GEOJSON_CACHE_KEY = "catchit:geojson:stops";
    private final StopRepository stopRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public GeoJsonGeneratorService(StopRepository stopRepository, RedisTemplate<String, Object> redisTemplate) {
        this.stopRepository = stopRepository;
        this.redisTemplate = redisTemplate;
    }

    // Automatically build the cache on startup
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCache() {
        logger.info("Initializing GeoJSON cache in Redis after application startup");
        Map<String, Object> cachedGeoJson = refreshCache();
        List<?> features = (List<?>) cachedGeoJson.getOrDefault("features", List.of());
        logger.info("GeoJSON cache ready in Redis with {} features", features.size());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getStopsGeoJson() {
        // Check Redis first for maximum speed
        Object cached = redisTemplate.opsForValue().get(GEOJSON_CACHE_KEY);
        if (cached != null) {
            logger.debug("Serving GeoJSON from Redis cache");
            return (Map<String, Object>) cached;
        }

        logger.info("GeoJSON cache miss in Redis, regenerating from database");
        return refreshCache();
    }

    public Map<String, Object> refreshCache() {
        logger.info("Generating GeoJSON from database and storing it in Redis");
        List<Stop> stops = stopRepository.findAll();
        List<Map<String, Object>> features = new ArrayList<>();

        for (Stop stop : stops) {
            if (stop.getLocation() == null) continue;

            Map<String, Object> feature = new HashMap<>();
            feature.put("type", "Feature");

            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", "Point");
            // GeoJSON order: [longitude, latitude]
            geometry.put("coordinates", new double[] { 
                stop.getLocation().getLongitude(), 
                stop.getLocation().getLatitude() 
            });
            feature.put("geometry", geometry);

            Map<String, Object> props = new HashMap<>();
            props.put("id", stop.getId().toString());
            props.put("name", stop.getName());
            props.put("code", stop.getStopCode());
            props.put("stopType", stop.getStopType());
            props.put("latitude", stop.getLocation().getLatitude());
            props.put("longitude", stop.getLocation().getLongitude());
            feature.put("properties", props);

            features.add(feature);
        }

        Map<String, Object> collection = new HashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("features", features);

        // Save to Redis for 24 hours
        redisTemplate.opsForValue().set(GEOJSON_CACHE_KEY, collection, 24, TimeUnit.HOURS);
        logger.info("GeoJSON cache stored in Redis with {} features", features.size());
        return collection;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        Object cached = redisTemplate.opsForValue().get(GEOJSON_CACHE_KEY);
        status.put("cached", cached != null);
        if (cached != null) {
            Map<String, Object> geoJson = (Map<String, Object>) cached;
            List<Map<String, Object>> features = (List<Map<String, Object>>) geoJson.get("features");
            status.put("featureCount", features.size());
        }
        logger.debug("GeoJSON cache status requested: {}", status);
        return status;
    }
}