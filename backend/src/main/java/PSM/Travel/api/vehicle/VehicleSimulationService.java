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
import PSM.Travel.VehicleType;
import jakarta.annotation.PostConstruct;

@Service
public class VehicleSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleSimulationService.class);
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static final long TIME_BETWEEN_STOPS_SECONDS = 180L;

    private final VehicleRepository vehicleRepository;
    private final PSM.Location.api.route.RouteRepository routeRepository;
    private final double speedFactor;
    private final VehicleService vehicleService;
    private final Map<UUID, VehicleTrack> tracksByVehicleId = new ConcurrentHashMap<>();
    private final Map<UUID, VehicleSimulationSnapshotDTO> latestSnapshots = new ConcurrentHashMap<>();
    
    private final Map<UUID, UUID> lastNotifiedStops = new ConcurrentHashMap<>();

    public VehicleSimulationService(
            VehicleRepository vehicleRepository,
            PSM.Location.api.route.RouteRepository routeRepository,
            VehicleService vehicleService,
            @Value("${simulation.vehicle.speed-factor:100.0}") double speedFactor) {
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.speedFactor = speedFactor;
        this.vehicleService = vehicleService;
    }

    @PostConstruct
    public void init() {
        rebuildTracks();
        recomputeSnapshots();
    }

    // @Scheduled(fixedDelayString = "${simulation.vehicle.refresh-ms:30000}")
    public void refreshTracks() {
        rebuildTracks();
    }

    // @Scheduled(fixedDelayString = "${simulation.vehicle.tick-ms:1000}")
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
                        String typeString = vehicle.getType();
                        VehicleType vType = null;
                        if (typeString != null) {
                            try {
                                vType = VehicleType.valueOf(typeString);
                            } catch (IllegalArgumentException e) {
                                logger.warn("Tipo de veículo inválido encontrado: {}", typeString);
                            }
                        }

                        // Passa o vType para o record VehicleTrack
                        rebuilt.put(vehicle.getId(), new VehicleTrack(vehicle.getId(), routeTrack, vType));
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
        if (track.routeTrack().points().size() < 2 || track.routeTrack().totalDurationSeconds() <= 0) {
            return null;
        }

        long routeSeconds = track.routeTrack().totalDurationSeconds();
        long vehicleOffsetSeconds = Math.floorMod(track.vehicleId().hashCode(), routeSeconds);
        long simulatedSeconds = (long) Math.floor((nowMillis / 1000.0) * Math.max(0.1d, this.speedFactor));
        long elapsedSeconds = Math.floorMod(simulatedSeconds + vehicleOffsetSeconds, routeSeconds);

        int segmentIndex = 0;
        for (int i = 0; i < track.routeTrack().points().size() - 1; i++) {
            if (elapsedSeconds >= track.routeTrack().points().get(i).offsetSeconds() && 
                elapsedSeconds <= track.routeTrack().points().get(i + 1).offsetSeconds()) {
                segmentIndex = i;
                break;
            }
        }

        TrackPoint previous = track.routeTrack().points().get(segmentIndex);
        TrackPoint next = track.routeTrack().points().get(segmentIndex + 1);

        long segmentDuration = next.offsetSeconds() - previous.offsetSeconds();
        double progress = 0.0;
        if (segmentDuration > 0) {
            progress = (double) (elapsedSeconds - previous.offsetSeconds()) / segmentDuration;
        }

        // ==================== OBSERVER PATTERN TRIGGER ====================
        double toleranceMargin = 0.15;

        if (progress <= toleranceMargin) {
            UUID lastNotifiedStopId = lastNotifiedStops.get(track.vehicleId());

            if (lastNotifiedStopId == null || !lastNotifiedStopId.equals(previous.stopId())) {
                
                lastNotifiedStops.put(track.vehicleId(), previous.stopId());

                try {
                    // logger.info("Simulador: Veículo {} [{}] chegou à paragem {}. Disparando notificação.", 
                    //         track.vehicleId(), track.vehicleType(), previous.stopName());
                            
                    this.vehicleService.arrive(
                        track.vehicleId(), 
                        previous.stopId(),
                        track.routeTrack().routeId(),
                        track.routeTrack().routeName(),
                        track.vehicleType()
                    );
                } catch (Exception e) {
                    lastNotifiedStops.remove(track.vehicleId());
                    logger.error("Erro ao processar chegada do veículo {} à paragem {}", 
                            track.vehicleId(), previous.stopId(), e);
                }
            }
        }
        // ==================================================================

        double latitude = lerp(previous.latitude(), next.latitude(), progress);
        double longitude = lerp(previous.longitude(), next.longitude(), progress);

        return new VehicleSimulationSnapshotDTO(
            track.vehicleId(),
            track.routeTrack().routeId(),
            track.routeTrack().routeName(),
            latitude,
            longitude,
            previous.stopId(),
            previous.stopName(),
            next.stopId(),
            next.stopName(),
            progress,
            OffsetDateTime.now(ZONE).toString(),
            track.vehicleType() != null ? track.vehicleType().name() : null
    );}

    private RouteTrack buildRouteTrack(Route route) {
        if (route.routeStops == null || route.routeStops.isEmpty()) {
            return null;
        }

        List<RouteStop> routeStops = new ArrayList<>(route.routeStops);
        routeStops.sort(Comparator.comparingInt(RouteStop::getSequence));

        List<TrackPoint> points = new ArrayList<>();
        long currentOffset = 0L;

        for (RouteStop routeStop : routeStops) {
            Stop stop = routeStop.getStop();
            if (stop == null || stop.getLocation() == null) {
                continue;
            }

            if (!points.isEmpty() && points.get(points.size() - 1).stopId().equals(stop.getId())) {
                continue;
            }

            if (!points.isEmpty()) {
                currentOffset += TIME_BETWEEN_STOPS_SECONDS;
            }

            points.add(new TrackPoint(
                    stop.getId(),
                    stop.getName(),
                    stop.getLocation().getLatitude(),
                    stop.getLocation().getLongitude(),
                    currentOffset
            ));
        }

        // if (points.size() < 2) {
        //     logger.warn("Route {} has only {} unique stops, skipping", route.getName(), points.size());
        //     return null;
        // }

        long totalDurationSeconds = points.get(points.size() - 1).offsetSeconds();

        // logger.info("Route {} has {} unique stops, total duration {} seconds",
        //         route.getName(), points.size(), totalDurationSeconds);

        return new RouteTrack(route.getId(), route.getName(), points, totalDurationSeconds);
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private record VehicleTrack(UUID vehicleId, RouteTrack routeTrack, VehicleType vehicleType) {}

    private record RouteTrack(UUID routeId, String routeName, List<TrackPoint> points, long totalDurationSeconds) {}

    private record TrackPoint(UUID stopId, String stopName, double latitude, double longitude, long offsetSeconds) {}
}