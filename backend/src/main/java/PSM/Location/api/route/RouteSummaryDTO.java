package PSM.Location.api.route;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RouteSummaryDTO(
    UUID id,
    String name,
    List<StopSummaryDTO> stops
) {
    public record StopSummaryDTO(
        UUID stopId,
        String stopName,
        String stopCode,
        String displayCode,
        String stopType
    ) {}

    public RouteSummaryDTO {
        if (stops == null) {
            stops = new ArrayList<>();
        }
    }
}
