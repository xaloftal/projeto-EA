package PSM.Travel.api.vehicle;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PSM.Location.Stop;
import PSM.Location.api.stop.StopService;
import PSM.UserManagement.User;
import PSM.UserManagement.notification.NotificationCacheService;
import PSM.UserManagement.api.user.UserRepository;
import PSM.Travel.Vehicle;

@Service
public class VehicleService {
    private final VehicleRepository repository;
    private final StopService stopService;
    private final UserRepository userRepository;
    private final NotificationCacheService notificationCacheService;

    public VehicleService(VehicleRepository repository, StopService stopService, UserRepository userRepository, NotificationCacheService notificationCacheService) {
        this.repository = repository;
        this.stopService = stopService;
        this.userRepository = userRepository;
        this.notificationCacheService = notificationCacheService;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Vehicle findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
    }

    @Transactional
    public Vehicle create(Vehicle entity) {
        return repository.save(entity);
    }

    @Transactional
    public Vehicle update(UUID id, Vehicle entity) {
        findById(id);
        return repository.save(entity);
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

	@Transactional
	public Vehicle arrive(UUID vehicleId, UUID stopId) {
		Vehicle vehicle = findById(vehicleId);
		Stop stop = stopService.findById(stopId);
		List<User> observers = userRepository.findObserversByStop(stop);

		observers.forEach(stop::addObserver);
		vehicle.setStop(stop);
		vehicle.setLocation(stop.getLocation());
		vehicle.arrived();

		repository.save(vehicle);
		userRepository.saveAll(observers);
        observers.stream().map(User::getId).forEach(notificationCacheService::evict);
		return vehicle;
	}
}
