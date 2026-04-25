package PSM.Location.api.stop;

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

import PSM.Location.Stop;

@RestController
@RequestMapping("/api/stops")
public class StopController {
    private final StopService service;

    public StopController(StopService service) {
        this.service = service;
    }

    @GetMapping
    public List<Stop> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Stop getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public Stop create(@RequestBody Stop entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public Stop update(@PathVariable UUID id, @RequestBody Stop entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
