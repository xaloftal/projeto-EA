package PSM.Travel.api.vehicle;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PSM.Location.Route;
import PSM.Location.RouteStop;
import PSM.Location.Stop;
import jakarta.annotation.PostConstruct;

@Service
public class VehicleSimulationService {
    private static final Logger logger = LoggerFactory.getLogger(VehicleSimulationService.class);
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final long DEFAULT_SEGMENT_SECONDS = 15L;

    private final VehicleRepository vehicleRepository;
    private final PSM.Location.api.route.RouteRepository routeRepository;
    private final double speedFactor;

    private final Map<UUID, VehicleTrack> tracksByVehicleId = new ConcurrentHashMap<>();
    private final Map<UUID, VehicleSimulationSnapshotDTO> latestSnapshots = new ConcurrentHashMap<>();

    public VehicleSimulationService(
            VehicleRepository vehicleRepository,
            PSM.Location.api.route.RouteRepository routeRepository,
            @Value("${simulation.vehicle.speed-factor:100.0}") double speedFactor) {
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.speedFactor = speedFactor;
    }

    @PostConstruct
    public void init() {
        rebuildTracks();
        recomputeSnapshots();
    }

    @Scheduled(fixedDelayString = "${simulation.vehicle.refresh-ms:30000}")
    public void refreshTracks() {
        rebuildTracks();
    }

    @Scheduled(fixedDelayString = "${simulation.vehicle.tick-ms:1000}")
    public void tick() {
        recomputeSnapshots();
    }

    @Transactional(readOnly = true)
    protected void rebuildTracks() {
        Map<UUID, VehicleTrack> rebuilt = new LinkedHashMap<>();

        List<Route> routes = routeRepository.findAllWithRouteStops();
        Map<UUID, RouteTrack> routeTracks = new LinkedHashMap<>();
        for (Route route : routes) {
            RouteTrack routeTrack = buildRouteTrack(route);
            if (routeTrack != null) {
                routeTracks.put(route.getId(), routeTrack);
            }
        }

        vehicleRepository.findAll().stream()
                .filter(vehicle -> vehicle.getRoute() != null)
                .forEach(vehicle -> {
                    RouteTrack routeTrack = routeTracks.get(vehicle.getRoute().getId());
                    if (routeTrack != null) {
                        rebuilt.put(vehicle.getId(), new VehicleTrack(vehicle.getId(), routeTrack));
                    }
                });

        tracksByVehicleId.clear();
        tracksByVehicleId.putAll(rebuilt);
        logger.debug("Rebuilt simulation tracks for {} vehicles", tracksByVehicleId.size());
    }

    public List<VehicleSimulationSnapshotDTO> getLatestSnapshots() {
        return latestSnapshots.values().stream()
                .sorted(Comparator.comparing(VehicleSimulationSnapshotDTO::vehicleId))
                .toList();
    }

    public VehicleSimulationSnapshotDTO getSnapshot(UUID vehicleId) {
        return Optional.ofNullable(latestSnapshots.get(vehicleId))
                .orElseThrow(() -> new RuntimeException("Vehicle simulation snapshot not found"));
    }

    private void recomputeSnapshots() {
        if (tracksByVehicleId.isEmpty()) {
            latestSnapshots.clear();
            return;
        }

        long nowMillis = System.currentTimeMillis();
        Map<UUID, VehicleSimulationSnapshotDTO> refreshed = new LinkedHashMap<>();

        for (VehicleTrack track : tracksByVehicleId.values()) {
            VehicleSimulationSnapshotDTO snapshot = computeSnapshot(track, nowMillis);
            if (snapshot != null) {
                refreshed.put(snapshot.vehicleId(), snapshot);
            }
        }

        latestSnapshots.clear();
        latestSnapshots.putAll(refreshed);
    }

    private VehicleSimulationSnapshotDTO computeSnapshot(VehicleTrack track, long nowMillis) {
        if (track.routeTrack.points.size() < 2 || track.routeTrack.totalDurationSeconds <= 0) {
            return null;
        }

        long routeSeconds = Math.max(1L, track.routeTrack.totalDurationSeconds);
        long vehicleOffsetSeconds = Math.floorMod(track.vehicleId.hashCode(), routeSeconds);
        long simulatedSeconds = (long) Math.floor((nowMillis / 1000.0) * Math.max(0.1d, this.speedFactor));
        long elapsedSeconds = Math.floorMod(simulatedSeconds + vehicleOffsetSeconds, routeSeconds);

        int segmentIndex = (int) Math.min(track.routeTrack.points.size() - 2, elapsedSeconds / DEFAULT_SEGMENT_SECONDS);
        double progress = (elapsedSeconds % DEFAULT_SEGMENT_SECONDS) / (double) DEFAULT_SEGMENT_SECONDS;

        TrackPoint previous = track.routeTrack.points.get(segmentIndex);
        TrackPoint next = track.routeTrack.points.get(segmentIndex + 1);
        double latitude = lerp(previous.latitude, next.latitude, progress);
        double longitude = lerp(previous.longitude, next.longitude, progress);

        return new VehicleSimulationSnapshotDTO(
                track.vehicleId,
                track.routeTrack.routeId,
                track.routeTrack.routeName,
                latitude,
                longitude,
                previous.stopId,
                previous.stopName,
                next.stopId,
                next.stopName,
                progress,
                OffsetDateTime.now(ZONE).toString());
    }

    private RouteTrack buildRouteTrack(Route route) {
        List<RouteStop> routeStops = new ArrayList<>(route.routeStops);
        logger.debug("Route {} has {} routeStops from DB", route.getName(), routeStops.size());
        routeStops.sort(Comparator.comparingInt(RouteStop::getSequence));

        List<TrackPoint> points = new ArrayList<>();
        long offsetSeconds = 0L;

        for (RouteStop routeStop : routeStops) {
            Stop stop = routeStop.getStop();
            if (stop == null) {
                logger.debug("  RouteStop has null stop");
                continue;
            }
            if (stop.getLocation() == null) {
                logger.debug("  Stop {} has null location", stop.getName());
                continue;
            }

            if (!points.isEmpty()) {
                TrackPoint last = points.get(points.size() - 1);
                if (last.stopId.equals(stop.getId())) {
                    continue;
                }

                offsetSeconds += DEFAULT_SEGMENT_SECONDS;
            }

            points.add(new TrackPoint(
                    stop.getId(),
                    stop.getName(),
                    stop.getLocation().getLatitude(),
                    stop.getLocation().getLongitude(),
                    offsetSeconds));
        }

        if (points.size() < 2) {
            logger.warn("Route {} has only {} unique stops, skipping", route.getName(), points.size());
            return null;
        }

        

        long totalDurationSeconds = Math.max(DEFAULT_SEGMENT_SECONDS, (long) (points.size() - 1) * DEFAULT_SEGMENT_SECONDS);
        logger.info("Route {} has {} unique stops, total duration {} seconds", route.getName(), points.size(), totalDurationSeconds);
        return new RouteTrack(route.getId(), route.getName(), points, totalDurationSeconds);
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private record VehicleTrack(UUID vehicleId, RouteTrack routeTrack) {}

    private record RouteTrack(UUID routeId, String routeName, List<TrackPoint> points, long totalDurationSeconds) {}

    private record TrackPoint(UUID stopId, String stopName, double latitude, double longitude, long offsetSeconds) {}
}