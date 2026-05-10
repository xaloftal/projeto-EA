package PSM.Location.api.route;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Location.Route;
import PSM.Location.RouteStop;
import PSM.Location.Stop;
import PSM.Location.StopSchedule;

@Service
public class RouteService {
    private final RouteRepository repository;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId APP_TIMEZONE = ZoneId.of("Europe/Lisbon");

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
            List<RouteStop> routeStops = new ArrayList<>(route.routeStops);
            routeStops.sort(Comparator.comparingInt(RouteStop::getSequence));

            int fromIndex = -1;
            for (int index = 0; index < routeStops.size(); index++) {
                Stop stop = routeStops.get(index).getStop();
                if (stop != null && fromStopId.equals(stop.getId())) {
                    fromIndex = index;
                    break;
                }
            }

            if (fromIndex < 0) {
                continue;
            }

            int toIndex = -1;
            for (int index = fromIndex + 1; index < routeStops.size(); index++) {
                Stop stop = routeStops.get(index).getStop();
                if (stop != null && toStopId.equals(stop.getId())) {
                    toIndex = index;
                    break;
                }
            }

            if (toIndex < 0) {
                continue;
            }

            RouteStop fromRouteStop = routeStops.get(fromIndex);
            RouteStop toRouteStop = routeStops.get(toIndex);

            StopSchedule fromSchedule = findSchedule(route.schedules, fromStopId, fromRouteStop.getSequence());
            StopSchedule toSchedule = findSchedule(route.schedules, toStopId, toRouteStop.getSequence());

            LocalTime departureTime = scheduleTime(fromSchedule);

            if (departureAfter != null && departureTime != null && departureTime.isBefore(departureAfter)) {
                continue;
            }

            LocalTime arrivalTime = scheduleTime(toSchedule);

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

    public List<StopRouteArrivalDTO> findStopArrivals(UUID stopId) {
        ZonedDateTime now = ZonedDateTime.now(APP_TIMEZONE);
        List<Route> routes = repository.findAllWithSchedulesByStopId(stopId);
        List<StopRouteArrivalDTO> arrivals = new ArrayList<>();

        for (Route route : routes) {
            OffsetDateTime nextArrivalAt = null;

            for (StopSchedule schedule : route.schedules) {
                if (schedule.stop == null || !stopId.equals(schedule.stop.getId())) {
                    continue;
                }

                LocalTime scheduleTime = scheduleTime(schedule);
                if (scheduleTime == null) {
                    continue;
                }

                OffsetDateTime candidate = nextOccurrence(scheduleTime, now);
                if (nextArrivalAt == null || candidate.isBefore(nextArrivalAt)) {
                    nextArrivalAt = candidate;
                }
            }

            if (nextArrivalAt != null) {
                arrivals.add(new StopRouteArrivalDTO(route.getId(), route.getName(), nextArrivalAt));
            }
        }

        arrivals.sort(Comparator
                .comparing(StopRouteArrivalDTO::nextArrivalAt)
                .thenComparing(arrival -> arrival.routeName() == null ? "" : arrival.routeName()));

        return arrivals;
    }

    public Route update(UUID id, Route entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private StopSchedule findSchedule(java.util.Collection<StopSchedule> schedules, UUID stopId, Integer sequence) {
        List<StopSchedule> candidates = schedules.stream()
                .filter(schedule -> schedule.stop != null && stopId.equals(schedule.stop.getId()))
                .toList();

        if (sequence != null) {
            List<StopSchedule> sequenceMatches = candidates.stream()
                    .filter(schedule -> schedule.getSequence() == sequence)
                    .toList();
            if (!sequenceMatches.isEmpty()) {
                candidates = sequenceMatches;
            }
        }

        return candidates.stream()
                .min(Comparator.comparing(this::scheduleTime).thenComparingInt(StopSchedule::getSequence))
                .orElse(null);
    }

    private LocalTime scheduleTime(StopSchedule schedule) {
        if (schedule == null) {
            return null;
        }

        if (schedule.getDepartureTime() != null) {
            return schedule.getDepartureTime().toLocalTime();
        }

        if (schedule.getArrivalTime() != null) {
            return schedule.getArrivalTime().toLocalTime();
        }

        return null;
    }

    private OffsetDateTime nextOccurrence(LocalTime time, ZonedDateTime now) {
        ZonedDateTime candidate = now.withHour(time.getHour())
                .withMinute(time.getMinute())
                .withSecond(time.getSecond())
                .withNano(0);

        // We always want the next pass, not the current/past one.
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }

        return candidate.toOffsetDateTime();
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

    public List<Object> findAllOptimized() {
        List<Route> routes = repository.findAllWithSchedules();
        List<Object> result = new ArrayList<>();

        for (Route route : routes) {
            RouteWithSchedulesDTO dto = new RouteWithSchedulesDTO(route.getId(), route.getName());

            if (route.schedules != null) {
                for (StopSchedule schedule : route.schedules) {
                    if (schedule.stop != null) {
                        double lat = schedule.stop.getLocation() != null ? schedule.stop.getLocation().getLatitude() : 0;
                        double lon = schedule.stop.getLocation() != null ? schedule.stop.getLocation().getLongitude() : 0;

                        dto.schedules.add(new RouteWithSchedulesDTO.ScheduleDTO(
                                schedule.stop.getId(),
                                schedule.stop.getName(),
                                lat,
                                lon,
                                schedule.getArrivalTime(),
                                schedule.getDepartureTime(),
                                schedule.getSequence()));
                    }
                }
            }

            if (!dto.schedules.isEmpty()) {
                result.add(dto);
            }
        }

        return result;
    }
}
