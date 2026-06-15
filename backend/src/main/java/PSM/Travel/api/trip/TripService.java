package PSM.Travel.api.trip;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import PSM.Location.Stop;
import PSM.Location.StopSchedule;
import PSM.Location.api.route.RouteStopDTO;
import PSM.Location.api.stop.StopDTO;
import PSM.Ticketing.Card;
import PSM.Ticketing.Ticket;
import PSM.Ticketing.Title;
import PSM.Ticketing.api.title.TitleRepository;
import PSM.Travel.Trip;

@Service
public class TripService {
    private final TripRepository repository;
    private final TitleRepository titleRepository;
    private static final ZoneId APP_TIMEZONE = ZoneId.of("Europe/Lisbon");

    public TripService(TripRepository repository, TitleRepository titleRepository) {
        this.repository = repository;
        this.titleRepository = titleRepository;
    }

    public List<Trip> findAll() {
        return repository.findAll();
    }

    public Trip findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Trip not found"));
    }

    public Trip create(Trip entity) {
        return repository.save(entity);
    }

    public Trip update(UUID id, Trip entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);

    }

    public List<Trip> findActiveTrips() {
        return repository.findActiveTripsWithRoute();
    }

    public List<Trip> findActiveTripsForTitle(UUID titleId) {
        Title title = titleRepository.findById(titleId)
                .orElseThrow(() -> new RuntimeException("Title not found"));

        if (title instanceof Ticket ticket) {
            return repository.findActiveTripsForStops(ticket.getFrom().getId(), ticket.getTo().getId());
        } else if (title instanceof Card card) {
            return repository.findActiveTripsForZone(card.getZone().getId());
        }
        
        return repository.findActiveTripsWithRoute();
    }

    public List<RouteStopDTO> findTripStops(UUID tripId, UUID currentStopId) {
        Trip trip = findById(tripId);
        if (trip.route == null)
            return List.of();

        LocalTime now = LocalTime.now(APP_TIMEZONE);

        // Agrupar schedules por sequence, ordenados por arrival_time
        Map<Integer, List<StopSchedule>> bySequence = trip.route.schedules.stream()
                .filter(s -> s.stop != null && scheduleTime(s) != null)
                .collect(Collectors.groupingBy(
                        StopSchedule::getSequence,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(s -> scheduleTime(s)))
                                        .collect(Collectors.toList()))));

        // Encontrar a sequence da paragem atual
        int currentSequence = trip.route.schedules.stream()
                .filter(s -> s.stop != null && s.stop.getId().equals(currentStopId))
                .mapToInt(StopSchedule::getSequence)
                .findFirst()
                .orElse(1);

        // Fixar âncora na paragem atual — horário mais próximo da hora atual
        List<StopSchedule> anchorGroup = bySequence.getOrDefault(currentSequence, List.of());
        StopSchedule anchorSchedule = anchorGroup.stream()
                .filter(s -> !scheduleTime(s).isBefore(now))
                .findFirst()
                .orElse(anchorGroup.isEmpty() ? null : anchorGroup.get(0));

        if (anchorSchedule == null)
            return List.of();

        int maxSequence = bySequence.keySet().stream().mapToInt(i -> i).max().orElse(1);

        // Reconstrução em cadeia para a frente (paragens futuras)
        List<RouteStopDTO> result = new ArrayList<>();
        LocalTime previousTime = scheduleTime(anchorSchedule);

        for (int seq = currentSequence; seq <= maxSequence; seq++) {
            List<StopSchedule> group = bySequence.getOrDefault(seq, List.of());
            LocalTime prevTime = previousTime;
            StopSchedule chosen = group.stream()
                    .filter(s -> !scheduleTime(s).isBefore(prevTime))
                    .findFirst()
                    .orElse(null);

            if (chosen == null)
                break;

            previousTime = scheduleTime(chosen);
            result.add(new RouteStopDTO(
                    chosen.stop.getId().toString(),
                    chosen.stop.getName(),
                    chosen.getSequence(),
                    previousTime.format(DateTimeFormatter.ofPattern("HH:mm"))));
        }

        // Reconstrução em cadeia para trás (paragens passadas)
        List<RouteStopDTO> passed = new ArrayList<>();
        LocalTime nextTime = scheduleTime(anchorSchedule);

        for (int seq = currentSequence - 1; seq >= 1; seq--) {
            List<StopSchedule> group = bySequence.getOrDefault(seq, List.of());
            LocalTime nTime = nextTime;
            StopSchedule chosen = group.stream()
                    .filter(s -> !scheduleTime(s).isAfter(nTime))
                    .reduce((first, second) -> second) // último antes de nextTime
                    .orElse(null);

            if (chosen == null)
                break;

            nextTime = scheduleTime(chosen);
            passed.add(0, new RouteStopDTO(
                    chosen.stop.getId().toString(),
                    chosen.stop.getName(),
                    chosen.getSequence(),
                    nextTime.format(DateTimeFormatter.ofPattern("HH:mm"))));
        }

        passed.addAll(result);
        return passed;
    }

    private LocalTime scheduleTime(StopSchedule s) {
        if (s.getDepartureTime() != null)
            return s.getDepartureTime().toLocalTime();
        if (s.getArrivalTime() != null)
            return s.getArrivalTime().toLocalTime();
        return null;
    }

public List<StopDTO> getTripStopsWithZones(UUID tripId) {
    Trip trip = findById(tripId);
    if (trip.getRoute() == null) {
        return List.of();
    }
    
    // Usar LinkedHashMap para garantir stops únicos por sequence
    Map<Integer, StopDTO> uniqueStops = new LinkedHashMap<>();
    
    for (StopSchedule schedule : trip.getRoute().getSchedules()) {
        if (schedule.getStop() != null) {
            int sequence = schedule.getSequence();
            if (!uniqueStops.containsKey(sequence)) {
                Stop stop = schedule.getStop();
                StopDTO dto = new StopDTO();
                dto.setId(stop.getId());
                dto.setName(stop.getName());
                dto.setStopCode(stop.getStopCode());
                dto.setSequence(sequence);
                if (stop.getZone() != null) {
                    dto.setZoneName(stop.getZone().getName());
                    dto.setZoneId(stop.getZone().getId());
                }
                if (stop.getLocation() != null) {
                    dto.setLatitude(stop.getLocation().getLatitude());
                    dto.setLongitude(stop.getLocation().getLongitude());
                }
                uniqueStops.put(sequence, dto);
            }
        }
    }
    
    return new ArrayList<>(uniqueStops.values());
}
}
