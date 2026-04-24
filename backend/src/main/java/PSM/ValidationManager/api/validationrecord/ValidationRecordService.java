package PSM.ValidationManager.api.validationrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.ValidationManager.ValidationRecord;

@Service
public class ValidationRecordService {
    private final ValidationRecordRepository repository;

    public ValidationRecordService(ValidationRecordRepository repository) {
        this.repository = repository;
    }

    public List<ValidationRecord> findAll() {
        return repository.findAll();
    }

    public ValidationRecord findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("ValidationRecord not found"));
    }

    public ValidationRecord create(ValidationRecord entity) {
        return repository.save(entity);
    }

    public ValidationRecord update(UUID id, ValidationRecord entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
