package PSM.Location.api.route;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StopRouteArrivalDTO(
        UUID routeId,
        String routeName,
        OffsetDateTime nextArrivalAt,
        String firstStopName,
        String lastStopName) {
}