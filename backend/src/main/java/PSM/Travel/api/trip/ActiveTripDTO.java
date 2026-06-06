package PSM.Travel.api.trip;


public record ActiveTripDTO(String id, String startTime, String routeName, java.util.List<String> stopIds, String zoneName) {}
