package PSM.ValidationManager.api.validationrecord;

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

import PSM.ValidationManager.ValidationRecord;

@RestController
@RequestMapping("/api/validationrecords")
public class ValidationRecordController {
    private final ValidationRecordService service;

    public ValidationRecordController(ValidationRecordService service) {
        this.service = service;
    }

    @GetMapping
    public List<ValidationRecord> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ValidationRecord getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ValidationRecord create(@RequestBody ValidationRecord entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public ValidationRecord update(@PathVariable UUID id, @RequestBody ValidationRecord entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
