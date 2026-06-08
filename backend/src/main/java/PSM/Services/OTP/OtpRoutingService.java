package PSM.Services.OTP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Calls OTP2 GraphQL, converts the resulting itinerary into a GeoJSON
 * FeatureCollection that the frontend can drop straight into Leaflet.
 * Results are cached in Redis keyed by (from, to, criteria).
 */
@Service
public class OtpRoutingService {

    private final WebClient webClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${otp.router-id}")
    private String routerId;

    @Value("${routing.cache-ttl-seconds}")
    private long cacheTtlSeconds;

    @Value("${routing.transfer-penalty}")
    private int transferPenalty;

    @Value("${routing.walk-reluctance}")
    private double walkReluctance;

    @Value("${routing.max-walk-distance-meters}")
    private int maxWalkDistance;

    public OtpRoutingService(
            @Value("${otp.base-url}") String otpBaseUrl,
            @Value("${otp.timeout-ms}") int timeoutMs,
            StringRedisTemplate redis) {
        this.webClient = WebClient.builder()
                .baseUrl(otpBaseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.redis = redis;
    }

    /**
     * Plan a route from one stop to another, optimizing for fewest transfers.
     *
     * @param fromLat origin latitude
     * @param fromLon origin longitude
     * @param toLat   destination latitude
     * @param toLon   destination longitude
     * @return GeoJSON FeatureCollection (as JsonNode) with one Feature per leg
     */
    public JsonNode planFewestTransfers(double fromLat, double fromLon,
            double toLat, double toLon) {
        String cacheKey = String.format("route:%.6f,%.6f:%.6f,%.6f:fewest",
                fromLat, fromLon, toLat, toLon);

        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return mapper.readTree(cached);
            } catch (Exception e) {
                /* fall through and recompute */ }
        }

        JsonNode otpResponse = callOtp(fromLat, fromLon, toLat, toLon);
        JsonNode geoJson = itineraryToGeoJson(otpResponse);

        try {
            redis.opsForValue().set(cacheKey, mapper.writeValueAsString(geoJson),
                    Duration.ofSeconds(cacheTtlSeconds));
        } catch (Exception ignored) {
        }

        return geoJson;
    }

    private JsonNode callOtp(double fromLat, double fromLon,
            double toLat, double toLon) {
        // We now have 2026 GTFS data, so we can use the actual current date and time!
        // The Metro GTFS you downloaded starts on 2026-04-06 and ends on 2026-07-19!
        String date = "2026-06-08";
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        // String time = "14:00:00";

        Map<String, Object> variables = Map.of(
                "fromLat", fromLat,
                "fromLon", fromLon,
                "toLat", toLat,
                "toLon", toLon,
                "date", date,
                "time", time,
                "transferPenalty", transferPenalty,
                "walkReluctance", walkReluctance,
                "maxWalkDistance", maxWalkDistance);

        Map<String, Object> body = Map.of(
                "query", PLAN_QUERY,
                "variables", variables);

        return webClient.post()
                .uri("/otp/routers/{routerId}/index/graphql", routerId)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    /**
     * Converts OTP's response into a GeoJSON FeatureCollection.
     * We pick the itinerary with the fewest transfers; ties broken by duration.
     */
    private JsonNode itineraryToGeoJson(JsonNode otpResponse) {
        ObjectNode featureCollection = mapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");
        ArrayNode features = mapper.createArrayNode();

        JsonNode itineraries = otpResponse
                .path("data").path("plan").path("itineraries");

        if (!itineraries.isArray() || itineraries.isEmpty()) {
            featureCollection.set("features", features);
            featureCollection.put("error", "no_route_found");
            return featureCollection;
        }

        JsonNode best = null;
        int bestTransfers = Integer.MAX_VALUE;
        long bestDuration = Long.MAX_VALUE;
        for (JsonNode it : itineraries) {
            int transitLegs = 0;
            for (JsonNode leg : it.path("legs")) {
                if (!"WALK".equalsIgnoreCase(leg.path("mode").asText())) {
                    transitLegs++;
                }
            }

            // If the itinerary has ZERO transit legs, it's a pure walk. We NEVER want to
            // show this.
            if (transitLegs == 0) {
                continue;
            }

            int t = Math.max(0, transitLegs - 1);
            long d = it.path("duration").asLong(Long.MAX_VALUE);
            if (t < bestTransfers || (t == bestTransfers && d < bestDuration)) {
                best = it;
                bestTransfers = t;
                bestDuration = d;
            }
        }

        // If we filtered out all the pure walking itineraries and nothing is left:
        if (best == null) {
            featureCollection.set("features", features);
            featureCollection.put("error", "no_route_found");
            return featureCollection;
        }

        ObjectNode summary = mapper.createObjectNode();
        summary.put("durationSeconds", best.path("duration").asLong());
        summary.put("transfers", bestTransfers);
        summary.put("walkDistanceMeters", best.path("walkDistance").asDouble());
        summary.put("startTime", best.path("startTime").asLong());
        summary.put("endTime", best.path("endTime").asLong());
        featureCollection.set("summary", summary);

        int legIndex = 0;
        for (JsonNode leg : best.path("legs")) {
            ArrayNode coords = decodePolyline(
                    leg.path("legGeometry").path("points").asText(""));
            if (coords.isEmpty()) {
                legIndex++;
                continue;
            }

            ObjectNode feature = mapper.createObjectNode();
            feature.put("type", "Feature");

            ObjectNode geom = mapper.createObjectNode();
            geom.put("type", "LineString");
            geom.set("coordinates", coords);
            feature.set("geometry", geom);

            ObjectNode props = mapper.createObjectNode();
            props.put("legIndex", legIndex++);
            props.put("mode", leg.path("mode").asText());
            props.put("routeShortName", leg.path("route").path("shortName").asText(""));
            props.put("routeLongName", leg.path("route").path("longName").asText(""));
            props.put("routeColor", leg.path("route").path("color").asText(""));
            props.put("fromStop", leg.path("from").path("name").asText());
            props.put("toStop", leg.path("to").path("name").asText());
            props.put("fromStopCode", leg.path("from").path("stop").path("code").asText(""));
            props.put("toStopCode", leg.path("to").path("stop").path("code").asText(""));
            props.put("startTime", leg.path("startTime").asLong());
            props.put("endTime", leg.path("endTime").asLong());
            props.put("distanceMeters", leg.path("distance").asDouble());
            props.put("isTransitLeg", !"WALK".equalsIgnoreCase(leg.path("mode").asText()));
            feature.set("properties", props);

            features.add(feature);
        }

        featureCollection.set("features", features);
        return featureCollection;
    }

    /**
     * Google encoded polyline → GeoJSON [lon, lat] array.
     * OTP uses precision 5, same as Google.
     */
    private ArrayNode decodePolyline(String encoded) {
        ArrayNode result = mapper.createArrayNode();
        if (encoded == null || encoded.isEmpty())
            return result;

        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, decoded = 0;
            do {
                b = encoded.charAt(index++) - 63;
                decoded |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((decoded & 1) != 0 ? ~(decoded >> 1) : (decoded >> 1));
            lat += dlat;

            shift = 0;
            decoded = 0;
            do {
                b = encoded.charAt(index++) - 63;
                decoded |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((decoded & 1) != 0 ? ~(decoded >> 1) : (decoded >> 1));
            lng += dlng;

            ArrayNode pair = mapper.createArrayNode();
            pair.add(lng / 1e5); // GeoJSON is [lon, lat]
            pair.add(lat / 1e5);
            result.add(pair);
        }
        return result;
    }

    private static final String PLAN_QUERY = """
            query Plan(
              $fromLat: Float!, $fromLon: Float!,
              $toLat: Float!, $toLon: Float!,
              $date: String!, $time: String!,
              $transferPenalty: Int!, $walkReluctance: Float!,
              $maxWalkDistance: Float!
            ) {
              plan(
                from: { lat: $fromLat, lon: $fromLon }
                to:   { lat: $toLat,   lon: $toLon }
                date: $date
                time: $time
                numItineraries: 5
                transferPenalty: $transferPenalty
                walkReluctance: $walkReluctance
                maxWalkDistance: $maxWalkDistance
                transportModes: [
                  { mode: WALK },
                  { mode: BUS },
                  { mode: RAIL },
                  { mode: SUBWAY },
                  { mode: TRAM }
                ]
              ) {
                itineraries {
                  duration
                  startTime
                  endTime
                  walkDistance
                  numberOfTransfers: legs { mode }  # placeholder, see note
                  legs {
                    mode
                    startTime
                    endTime
                    distance
                    from { name lat lon stop { code name } }
                    to   { name lat lon stop { code name } }
                    route { shortName longName color mode }
                    legGeometry { points length }
                  }
                }
              }
            }
            """;

    // private int countTransfers(JsonNode legs) {
    // int transit = 0;
    // for (JsonNode leg : legs) {
    // if (!"WALK".equalsIgnoreCase(leg.path("mode").asText()))
    // transit++;
    // }
    // return Math.max(0, transit - 1);
    // }
}
