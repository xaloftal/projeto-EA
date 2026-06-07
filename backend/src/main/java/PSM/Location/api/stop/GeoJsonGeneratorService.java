package PSM.Location.api.stop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import PSM.Location.Route;
import PSM.Location.Stop;
import PSM.Location.api.route.RouteRepository;

@Service
public class GeoJsonGeneratorService {
    private static final Logger logger = LoggerFactory.getLogger(GeoJsonGeneratorService.class);
    private static final String GEOJSON_CACHE_KEY = "catchit:geojson:stops";
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public GeoJsonGeneratorService(StopRepository stopRepository, RouteRepository routeRepository, RedisTemplate<String, Object> redisTemplate) {
        this.stopRepository = stopRepository;
        this.routeRepository = routeRepository;
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

    /**
     * Sobrecarga sem argumentos: Mantém a compatibilidade com chamadas antigas
     * e com o arranque da aplicação (initializeCache).
     */
    public Map<String, Object> getStopsGeoJson() {
        return getStopsGeoJson(null);
    }

    /**
     * Método principal: Se receber um routeId, filtra dinamicamente na hora.
     * Se for null, vai buscar a lista global rápida do Redis.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStopsGeoJson(UUID routeId) {
        // CÉNARIO 1: Sem filtro de rota -> Comportamento original super rápido com Redis
        if (routeId == null) {
            Object cached = redisTemplate.opsForValue().get(GEOJSON_CACHE_KEY);
            if (cached != null) {
                logger.debug("Serving global GeoJSON from Redis cache");
                return (Map<String, Object>) cached;
            }

            logger.info("GeoJSON cache miss in Redis, regenerating from database");
            return refreshCache();
        }

        logger.info("Filtering GeoJSON stops for routeId: {}", routeId);
        
        Optional<Route> routeOpt = routeRepository.findById(routeId);
        if (routeOpt.isEmpty()) {
            logger.warn("Route not found with ID: {}, returning empty collection", routeId);
            return createEmptyFeatureCollection();
        }

        Route route = routeOpt.get();
        Set<UUID> allowedStopIds = new HashSet<>();
        
        if (route.schedules != null) {
            route.schedules.forEach(schedule -> {
                if (schedule.stop != null) {
                    allowedStopIds.add(schedule.stop.getId());
                }
            });
        }

        Map<String, Object> globalGeoJson = getStopsGeoJson(null);
        List<Map<String, Object>> globalFeatures = (List<Map<String, Object>>) globalGeoJson.getOrDefault("features", new ArrayList<>());

        List<Map<String, Object>> filteredFeatures = globalFeatures.stream()
            .filter(feature -> {
                Map<String, Object> props = (Map<String, Object>) feature.get("properties");
                if (props == null || props.get("id") == null) return false;
                try {
                    UUID stopId = UUID.fromString((String) props.get("id"));
                    return allowedStopIds.contains(stopId);
                } catch (IllegalArgumentException e) {
                    return false;
                }
            })
            .collect(Collectors.toList());

        Map<String, Object> collection = new HashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("features", filteredFeatures);
        
        logger.info("Returned {} filtered stops for route {}", filteredFeatures.size(), route.getName());
        return collection;
    }

    private Map<String, Object> createEmptyFeatureCollection() {
        Map<String, Object> collection = new HashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("features", new ArrayList<>());
        return collection;
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
            props.put("stopType", stop.getStopType() != null ? stop.getStopType().toString() : "STOP");
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