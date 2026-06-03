package PSM.Location.api.route;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import PSM.Location.Route;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RouteService service;

    public RouteController(RouteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Route> getAll() {
        return service.findAll();
    }

    @GetMapping("/stop-arrivals")
    public List<StopRouteArrivalDTO> getStopArrivals(@RequestParam UUID stopId) {
        return service.findStopArrivals(stopId);
    }

    @GetMapping("/schedules")
    public List<RouteWithSchedulesDTO> getSchedules() {
        return service.findAllOptimized();
    }

    @GetMapping("/search")
    public List<RouteSearchResultDTO> search(
            @RequestParam UUID fromStopId,
            @RequestParam UUID toStopId,
            @RequestParam(required = false) String departureTime) {
        LocalTime parsedDepartureTime = null;
        if (departureTime != null && !departureTime.isBlank()) {
            try {
                parsedDepartureTime = LocalTime.parse(departureTime);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid departureTime format. Expected HH:mm or HH:mm:ss");
            }
        }

        return service.search(fromStopId, toStopId, parsedDepartureTime);
    }

    @GetMapping("/{id}")
    public Route getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public Route create(@RequestBody Route entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public Route update(@PathVariable UUID id, @RequestBody Route entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
