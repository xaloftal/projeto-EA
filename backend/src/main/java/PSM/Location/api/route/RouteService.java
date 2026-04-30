package PSM.Location.api.route;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import PSM.Location.Route;
import PSM.Location.Stop;
import PSM.Location.StopSchedule;
import org.springframework.stereotype.Service;

@Service
public class RouteService {
    private final RouteRepository repository;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public RouteService(RouteRepository repository) {
        this.repository = repository;
    }

    public List<Route> findAll() {
        return repository.findAll();
    }

    public Route findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Route not found"));
    }

    public Route create(Route entity) {
        return repository.save(entity);
    }

    public List<RouteSearchResultDTO> search(UUID fromStopId, UUID toStopId, LocalTime departureAfter) {
        List<RouteSearchResultDTO> results = new ArrayList<>();

        List<Route> routes = repository.findAll();
        for (int routeIndex = 0; routeIndex < routes.size(); routeIndex++) {
            Route route = routes.get(routeIndex);
            List<StopSchedule> schedules = new ArrayList<>(route.schedules);
            schedules.sort(Comparator.comparingInt(StopSchedule::getSequence));

            int fromIndex = -1;
            for (int index = 0; index < schedules.size(); index++) {
                Stop stop = schedules.get(index).stop;
                if (stop != null && fromStopId.equals(stop.getId())) {
                    fromIndex = index;
                    break;
                }
            }

            if (fromIndex < 0) {
                continue;
            }

            int toIndex = -1;
            for (int index = fromIndex + 1; index < schedules.size(); index++) {
                Stop stop = schedules.get(index).stop;
                if (stop != null && toStopId.equals(stop.getId())) {
                    toIndex = index;
                    break;
                }
            }

            if (toIndex < 0) {
                continue;
            }

            StopSchedule fromSchedule = schedules.get(fromIndex);
            StopSchedule toSchedule = schedules.get(toIndex);

            LocalTime departureTime = fromSchedule.getDepartureTime() != null
                    ? fromSchedule.getDepartureTime().toLocalTime()
                    : fromSchedule.getArrivalTime() != null ? fromSchedule.getArrivalTime().toLocalTime() : null;

            if (departureAfter != null && departureTime != null && departureTime.isBefore(departureAfter)) {
                continue;
            }

            LocalTime arrivalTime = toSchedule.getArrivalTime() != null
                    ? toSchedule.getArrivalTime().toLocalTime()
                    : toSchedule.getDepartureTime() != null ? toSchedule.getDepartureTime().toLocalTime() : null;

            int stopSpan = Math.max(1, toIndex - fromIndex);

            results.add(new RouteSearchResultDTO(
                    route.getId(),
                    route.getName(),
                    toStopDto(fromSchedule.stop),
                    toStopDto(toSchedule.stop),
                    departureTime != null ? departureTime.format(HH_MM) : "00:00",
                    arrivalTime != null ? arrivalTime.format(HH_MM) : "00:00",
                    2.8 + stopSpan * 0.75 + routeIndex * 0.1));
        }

        results.sort(Comparator.comparing(RouteSearchResultDTO::departureTime));
        return results;
    }

    public Route update(UUID id, Route entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private RouteSearchResultDTO.StopSearchResultDTO toStopDto(Stop stop) {
        double latitude = 0;
        double longitude = 0;

        if (stop.getLocation() != null) {
            latitude = stop.getLocation().getLatitude();
            longitude = stop.getLocation().getLongitude();
        }

        return new RouteSearchResultDTO.StopSearchResultDTO(
                stop.getId(),
                stop.getName(),
                latitude,
                longitude);
    }
}
