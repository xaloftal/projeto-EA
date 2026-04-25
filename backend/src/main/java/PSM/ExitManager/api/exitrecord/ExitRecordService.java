package PSM.ExitManager.api.exitrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.ExitManager.ExitRecord;

@Service
public class ExitRecordService {
    private final ExitRecordRepository repository;

    public ExitRecordService(ExitRecordRepository repository) {
        this.repository = repository;
    }

    public List<ExitRecord> findAll() {
        return repository.findAll();
    }

    public ExitRecord findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("ExitRecord not found"));
    }

    public ExitRecord create(ExitRecord entity) {
        return repository.save(entity);
    }

    public ExitRecord update(UUID id, ExitRecord entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
