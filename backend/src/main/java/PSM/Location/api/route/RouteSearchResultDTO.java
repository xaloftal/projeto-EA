package PSM.Location.api.route;

import java.util.UUID;

public record RouteSearchResultDTO(
        UUID routeId,
        String routeName,
        StopSearchResultDTO fromStop,
        StopSearchResultDTO toStop,
        String departureTime,
        String arrivalTime,
        double price) {

    public record StopSearchResultDTO(
            UUID id,
            String name,
            double latitude,
            double longitude) {
    }
}
