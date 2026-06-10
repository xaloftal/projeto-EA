package PSM.Location.api.route;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import PSM.Location.Route;
import PSM.Location.RouteStop;
import PSM.Location.Stop;
import PSM.Location.StopSchedule;
import PSM.Location.api.stopschedule.StopScheduleRepository;

@Service
public class RouteService {
    private final RouteRepository repository;
    private final StopScheduleRepository stopScheduleRepository;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId APP_TIMEZONE = ZoneId.of("Europe/Lisbon");

    public RouteService(RouteRepository repository, StopScheduleRepository stopScheduleRepository) {
        this.repository = repository;
        this.stopScheduleRepository = stopScheduleRepository;
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

        Map<UUID, List<StopSchedule>> schedulesByRoute = new LinkedHashMap<>();
        for (StopSchedule schedule : stopScheduleRepository.findAllByStopIdInWithRoute(List.of(fromStopId, toStopId))) {
            if (schedule.route == null || schedule.stop == null) {
                continue;
            }

            schedulesByRoute.computeIfAbsent(schedule.route.getId(), key -> new ArrayList<>()).add(schedule);
        }

        for (List<StopSchedule> routeSchedules : schedulesByRoute.values()) {
            Route route = routeSchedules.get(0).route;
            if (route == null) {
                continue;
            }

            StopSchedule fromSchedule = findSchedule(routeSchedules, fromStopId);
            StopSchedule toSchedule = findSchedule(routeSchedules, toStopId);

            if (fromSchedule == null || toSchedule == null) {
                continue;
            }

            if (fromSchedule.getSequence() >= toSchedule.getSequence()) {
                continue;
            }

            LocalTime departureTime = scheduleTime(fromSchedule);

            if (departureAfter != null && departureTime != null && departureTime.isBefore(departureAfter)) {
                continue;
            }

            LocalTime arrivalTime = scheduleTime(toSchedule);

            int stopSpan = Math.max(1, toSchedule.getSequence() - fromSchedule.getSequence());

            results.add(new RouteSearchResultDTO(
                    route.getId(),
                    route.getName(),
                    toStopDto(fromSchedule.stop),
                    toStopDto(toSchedule.stop),
                    departureTime != null ? departureTime.format(HH_MM) : "00:00",
                    arrivalTime != null ? arrivalTime.format(HH_MM) : "00:00",
                    2.8 + stopSpan * 0.75));
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
                String firstStop = route.routeStops.isEmpty() ? null : 
                    route.routeStops.stream()
                        .min(Comparator.comparingInt(RouteStop::getSequence))
                        .map(rs -> rs.getStop().getName())
                        .orElse(null);
                
                String lastStop = route.routeStops.isEmpty() ? null :
                    route.routeStops.stream()
                        .max(Comparator.comparingInt(RouteStop::getSequence))
                        .map(rs -> rs.getStop().getName())
                        .orElse(null);

                arrivals.add(new StopRouteArrivalDTO(route.getId(), route.getName(), nextArrivalAt, firstStop, lastStop));
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

    private StopSchedule findSchedule(java.util.Collection<StopSchedule> schedules, UUID stopId) {
        List<StopSchedule> candidates = schedules.stream()
                .filter(schedule -> schedule.stop != null && stopId.equals(schedule.stop.getId()))
                .toList();

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
                stop.getStopCode(),
                latitude,
                longitude);
    }

    public List<RouteWithSchedulesDTO> findAllOptimized() {
        List<Route> routes = repository.findAllWithSchedules();
        List<RouteWithSchedulesDTO> result = new ArrayList<>();

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
                                schedule.stop.getStopCode(),
                                schedule.stop.getStopType() != null ? schedule.stop.getStopType().name() : null,
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

    public List<RouteSummaryDTO> getRouteSummaries() {
        List<Route> routes = repository.findAllWithRouteStops();
        List<RouteSummaryDTO> summaries = new ArrayList<>();

        for (Route route : routes) {
            List<RouteSummaryDTO.StopSummaryDTO> stops = new ArrayList<>();
            java.util.Set<UUID> seenStops = new java.util.HashSet<>();
            
            if (route.getRouteStops() != null) {
                route.getRouteStops().stream()
                    .sorted(Comparator.comparingInt(rs -> rs.getSequence() != null ? rs.getSequence() : 0))
                    .forEach(rs -> {
                        if (rs.getStop() != null && seenStops.add(rs.getStop().getId())) {
                            stops.add(new RouteSummaryDTO.StopSummaryDTO(
                                rs.getStop().getId(),
                                rs.getStop().getName(),
                                rs.getStop().getStopCode(),
                                rs.getStop().getStopType() != null ? rs.getStop().getStopType().name() : null
                            ));
                        }
                    });
            }
            if (!stops.isEmpty()) {
                summaries.add(new RouteSummaryDTO(route.getId(), route.getName(), stops));
            }
        }
        return summaries;
    }

    public RouteWithSchedulesDTO findRouteScheduleOptimized(UUID routeId) {
        Route route = repository.findByIdWithSchedules(routeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));
            
        RouteWithSchedulesDTO dto = new RouteWithSchedulesDTO(route.getId(), route.getName());
        if (route.schedules != null) {
            for (StopSchedule schedule : route.schedules) {
                if (schedule.stop != null) {
                    double lat = schedule.stop.getLocation() != null ? schedule.stop.getLocation().getLatitude() : 0;
                    double lon = schedule.stop.getLocation() != null ? schedule.stop.getLocation().getLongitude() : 0;
                    dto.schedules.add(new RouteWithSchedulesDTO.ScheduleDTO(
                            schedule.stop.getId(),
                            schedule.stop.getName(),
                            schedule.stop.getStopCode(),
                            schedule.stop.getStopType() != null ? schedule.stop.getStopType().name() : null,
                            lat,
                            lon,
                            schedule.getArrivalTime(),
                            schedule.getDepartureTime(),
                            schedule.getSequence()));
                }
            }
        }
        return dto;
    }

    public List<RouteWithSchedulesDTO> findStopScheduleOptimized(UUID stopId) {
        List<Route> routes = repository.findAllWithSchedulesByStopId(stopId);
        List<RouteWithSchedulesDTO> result = new ArrayList<>();

        for (Route route : routes) {
            RouteWithSchedulesDTO dto = new RouteWithSchedulesDTO(route.getId(), route.getName());
            if (route.schedules != null) {
                for (StopSchedule schedule : route.schedules) {
                    if (schedule.stop != null && schedule.stop.getId().equals(stopId)) {
                        double lat = schedule.stop.getLocation() != null ? schedule.stop.getLocation().getLatitude() : 0;
                        double lon = schedule.stop.getLocation() != null ? schedule.stop.getLocation().getLongitude() : 0;
                        dto.schedules.add(new RouteWithSchedulesDTO.ScheduleDTO(
                                schedule.stop.getId(),
                                schedule.stop.getName(),
                                schedule.stop.getStopCode(),
                                schedule.stop.getStopType() != null ? schedule.stop.getStopType().name() : null,
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

    // Gets all the stops from a route (used to show the stops of a vehicle in the map view)
    public List<RouteStopDTO> findRouteStops(UUID routeId) {
        Route route = findById(routeId);
        LocalTime now = LocalTime.now(APP_TIMEZONE);
        
        return route.schedules.stream()
            .filter(s -> s.stop != null)
            .collect(Collectors.toMap(
                s -> s.stop.getId(),
                s -> s,
                (existing, replacement) -> {
                    LocalTime existingTime = scheduleTime(existing);
                    LocalTime replacementTime = scheduleTime(replacement);
                    if (existingTime == null) return replacement;
                    if (replacementTime == null) return existing;
                    return existingTime.isBefore(replacementTime) ? existing : replacement;
                },
                java.util.LinkedHashMap::new
            ))
            .values()
            .stream()
            .sorted(Comparator.comparingInt(StopSchedule::getSequence))
            .map(s -> new RouteStopDTO(
                s.stop.getId().toString(),
                s.stop.getName(),
                s.getSequence(),
                s.getArrivalTime() != null ? s.getArrivalTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "--"
            ))
            .toList();
    }
}
