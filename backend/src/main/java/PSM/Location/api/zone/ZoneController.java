package PSM.Location.api.zone;

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

import PSM.Location.Zone;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {
    private final ZoneService service;

    public ZoneController(ZoneService service) {
        this.service = service;
    }

    @GetMapping
    public List<Zone> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Zone getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public Zone create(@RequestBody Zone entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public Zone update(@PathVariable UUID id, @RequestBody Zone entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
