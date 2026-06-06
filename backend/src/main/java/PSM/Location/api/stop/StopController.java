package PSM.Location.api.stop;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PSM.Location.Stop;
import PSM.UserManagement.User;

@RestController
@RequestMapping("/api/stops")
public class StopController {
    private final StopService service;
    private final GeoJsonGeneratorService geoJsonGeneratorService;

    public StopController(StopService service, GeoJsonGeneratorService geoJsonGeneratorService) {
        this.service = service;
        this.geoJsonGeneratorService = geoJsonGeneratorService;
    }

    @GetMapping
    public List<Stop> getAll() {
        return service.findAll();
    }

/**
     * Returns stops as a GeoJSON FeatureCollection.
     * If routeId is provided, returns only the stops belonging to that route (Bypasses Redis cache).
     * If no routeId is provided, returns the pre-cached global GeoJSON from Redis.
     *
     * @param routeId Optional UUID of the route to filter stops by
     * @return GeoJSON FeatureCollection
     */
    @GetMapping("/geojson")
    public Map<String, Object> getStopsAsGeoJson(@org.springframework.web.bind.annotation.RequestParam(required = false) UUID routeId) {
        return geoJsonGeneratorService.getStopsGeoJson(routeId);
    }

    /**
     * Refreshes the GeoJSON cache in Redis.
     * Useful when stops are updated and you want immediate map updates.
     * This endpoint triggers a full regeneration of the cache from database.
     *
     * @return Updated GeoJSON FeatureCollection and cache info
     */
    @PostMapping("/geojson/refresh")
    public Map<String, Object> refreshGeoJsonCache() {
        geoJsonGeneratorService.refreshCache();
        Map<String, Object> status = geoJsonGeneratorService.getCacheStatus();
        status.put("success", true);
        status.put("message", "GeoJSON cache refreshed in Redis");
        return status;
    }

    /**
     * Gets cache status information.
     * Useful for debugging and monitoring Redis cache health.
     *
     * @return Cache status details
     */
    @GetMapping("/geojson/status")
    public Map<String, Object> getGeoJsonCacheStatus() {
        return geoJsonGeneratorService.getCacheStatus();
    }

    @GetMapping("/{id}")
    public Stop getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public Stop create(@RequestBody Stop entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public Stop update(@PathVariable UUID id, @RequestBody Stop entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/{id}/observers")
    public List<User> getObservers(@PathVariable UUID id) {
        return service.getObservers(id);
    }

    @PostMapping("/{stopId}/observers/{userId}")
    public void addObserver(@PathVariable UUID stopId, @PathVariable UUID userId) {
        service.addObserver(stopId, userId);
    }

    @DeleteMapping("/{stopId}/observers/{userId}")
    public void removeObserver(@PathVariable UUID stopId, @PathVariable UUID userId) {
        service.removeObserver(stopId, userId);
    }

    @PostMapping("/{id}/notify")
    public int notifyObservers(@PathVariable UUID id) {
        return service.notifyObservers(id);
    }
}
