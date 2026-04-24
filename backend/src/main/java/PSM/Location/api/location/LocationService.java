package PSM.Location.api.location;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Location.Location;

@Service
public class LocationService {
    private final LocationRepository repository;

    public LocationService(LocationRepository repository) {
        this.repository = repository;
    }

    public List<Location> findAll() {
        return repository.findAll();
    }

    public Location findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Location not found"));
    }

    public Location create(Location entity) {
        return repository.save(entity);
    }

    public Location update(UUID id, Location entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
