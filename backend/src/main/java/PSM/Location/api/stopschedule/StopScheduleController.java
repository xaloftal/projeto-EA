package PSM.Location.api.stopschedule;

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

import PSM.Location.StopSchedule;

@RestController
@RequestMapping("/api/stopschedules")
public class StopScheduleController {
    private final StopScheduleService service;

    public StopScheduleController(StopScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public List<StopSchedule> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public StopSchedule getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public StopSchedule create(@RequestBody StopSchedule entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public StopSchedule update(@PathVariable UUID id, @RequestBody StopSchedule entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
