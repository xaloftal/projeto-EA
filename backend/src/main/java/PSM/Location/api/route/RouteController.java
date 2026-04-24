package PSM.Location.api.route;

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
