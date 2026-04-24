package PSM.Travel.api.vehicle;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Travel.Vehicle;

@Service
public class VehicleService {
    private final VehicleRepository repository;

    public VehicleService(VehicleRepository repository) {
        this.repository = repository;
    }

    public List<Vehicle> findAll() {
        return repository.findAll();
    }

    public Vehicle findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
    }

    public Vehicle create(Vehicle entity) {
        return repository.save(entity);
    }

    public Vehicle update(UUID id, Vehicle entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
