package PSM.Location.api.route;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RouteWithSchedulesDTO {
    public UUID id;
    public String name;
    public List<ScheduleDTO> schedules;

    public RouteWithSchedulesDTO(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.schedules = new ArrayList<>();
    }

    public static class ScheduleDTO {
        public UUID stopId;
        public String stopName;
        public Double latitude;
        public Double longitude;
        public LocalDateTime arrivalTime;
        public LocalDateTime departureTime;
        public Integer sequence;

        public ScheduleDTO(UUID stopId, String stopName, Double latitude, Double longitude,
                          LocalDateTime arrivalTime, LocalDateTime departureTime, Integer sequence) {
            this.stopId = stopId;
            this.stopName = stopName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.arrivalTime = arrivalTime;
            this.departureTime = departureTime;
            this.sequence = sequence;
        }
    }
}
