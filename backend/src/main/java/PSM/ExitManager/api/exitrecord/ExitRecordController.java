package PSM.ExitManager.api.exitrecord;

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

import PSM.ExitManager.ExitRecord;

@RestController
@RequestMapping("/api/exitrecords")
public class ExitRecordController {
    private final ExitRecordService service;

    public ExitRecordController(ExitRecordService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExitRecord> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ExitRecord getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ExitRecord create(@RequestBody ExitRecord entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public ExitRecord update(@PathVariable UUID id, @RequestBody ExitRecord entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
