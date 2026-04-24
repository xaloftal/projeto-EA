package PSM.Location.api.route;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Location.Route;

@Service
public class RouteService {
    private final RouteRepository repository;

    public RouteService(RouteRepository repository) {
        this.repository = repository;
    }

    public List<Route> findAll() {
        return repository.findAll();
    }

    public Route findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Route not found"));
    }

    public Route create(Route entity) {
        return repository.save(entity);
    }

    public Route update(UUID id, Route entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
