package PSM.Location.api.zone;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import PSM.Location.Zone;

@Service
public class ZoneService {
    private final ZoneRepository repository;

    public ZoneService(ZoneRepository repository) {
        this.repository = repository;
    }

    public List<Zone> findAll() {
        return repository.findAll().stream()
                .filter(zone -> zone.getName() == null || !"OUT".equalsIgnoreCase(zone.getName().trim()))
                .collect(Collectors.toList());
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
