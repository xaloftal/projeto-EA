package PSM.Location.api.stop;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PSM.Location.Stop;
import PSM.UserManagement.User;
import PSM.UserManagement.notification.NotificationCacheService;
import PSM.UserManagement.api.user.UserRepository;

@Service
public class StopService {
    private final StopRepository repository;
    private final UserRepository userRepository;
	private final NotificationCacheService notificationCacheService;

	public StopService(StopRepository repository, UserRepository userRepository, NotificationCacheService notificationCacheService) {
        this.repository = repository;
        this.userRepository = userRepository;
		this.notificationCacheService = notificationCacheService;
    }

    @Transactional(readOnly = true)
    public List<Stop> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Stop findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Stop not found"));
    }

    @Transactional
    public Stop create(Stop entity) {
        return repository.save(entity);
    }

    @Transactional
    public Stop update(UUID id, Stop entity) {
        findById(id);
        return repository.save(entity);
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

	@Transactional(readOnly = true)
	public List<User> getObservers(UUID stopId) {
		Stop stop = findById(stopId);
		return userRepository.findObserversByStop(stop);
	}

	@Transactional
	public void addObserver(UUID stopId, UUID userId) {
		Stop stop = findById(stopId);
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		user.addPOI(stop);
		userRepository.save(user);
	}

	@Transactional
	public void removeObserver(UUID stopId, UUID userId) {
		Stop stop = findById(stopId);
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		user.removePOI(stop);
		userRepository.save(user);
	}

	@Transactional
	public int notifyObservers(UUID stopId) {
		Stop stop = findById(stopId);
		List<User> observers = userRepository.findObserversByStop(stop);
		observers.forEach(stop::addObserver);
		stop.notifyObservers();
		userRepository.saveAll(observers);
		observers.stream().map(User::getId).forEach(notificationCacheService::evict);
		return observers.size();
	}
}
