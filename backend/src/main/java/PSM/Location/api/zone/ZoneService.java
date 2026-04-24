package PSM.Location.api.zone;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Location.Zone;

@Service
public class ZoneService {
    private final ZoneRepository repository;

    public ZoneService(ZoneRepository repository) {
        this.repository = repository;
    }

    public List<Zone> findAll() {
        return repository.findAll();
    }

    public Zone findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Zone not found"));
    }

    public Zone create(Zone entity) {
        return repository.save(entity);
    }

    public Zone update(UUID id, Zone entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
