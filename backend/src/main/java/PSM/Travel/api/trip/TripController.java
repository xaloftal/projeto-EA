package PSM.Travel.api.trip;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PSM.Travel.Trip;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    @GetMapping
    public List<Trip> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Trip getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/active")
    public List<ActiveTripDTO> getActiveTrips() {
        return service.findActiveTrips().stream()
            .map(t -> {
                String routeName = t.route != null ? t.route.getName() : "NULL_ROUTE";
                System.out.println("Trip " + t.getId() + " route: " + routeName);
                return new ActiveTripDTO(
                    t.getId().toString(),
                    t.getStartTime().toString(),
                    routeName
                );
            })
            .toList();
    }

    @PostMapping
    public Trip create(@RequestBody Trip entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public Trip update(@PathVariable UUID id, @RequestBody Trip entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
