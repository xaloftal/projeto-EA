package PSM.Location.api.stop;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Location.Stop;

@Service
public class StopService {
    private final StopRepository repository;

    public StopService(StopRepository repository) {
        this.repository = repository;
    }

    public List<Stop> findAll() {
        return repository.findAll();
    }

    public Stop findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Stop not found"));
    }

    public Stop create(Stop entity) {
        return repository.save(entity);
    }

    public Stop update(UUID id, Stop entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
