package PSM.Travel.api.vehicle;

import java.util.UUID;

public record VehicleSimulationSnapshotDTO(
        UUID vehicleId,
        UUID routeId,
        String routeName,
        double latitude,
        double longitude,
        UUID previousStopId,
        String previousStopName,
        UUID nextStopId,
        String nextStopName,
        double progress,
        String updatedAt) {
}