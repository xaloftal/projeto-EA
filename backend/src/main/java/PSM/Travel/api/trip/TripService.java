package PSM.Travel.api.trip;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Travel.Trip;

@Service
public class TripService {
    private final TripRepository repository;

    public TripService(TripRepository repository) {
        this.repository = repository;
    }

    public List<Trip> findAll() {
        return repository.findAll();
    }

    public Trip findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Trip not found"));
    }

    public Trip create(Trip entity) {
        return repository.save(entity);
    }

    public Trip update(UUID id, Trip entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
 
    }

    public List<Trip> findActiveTrips() {
        return repository.findActiveTripsWithRoute();
    }
}
