package PSM.Travel.api.vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PSM.Location.Stop;
import PSM.Location.api.stop.StopService;
import PSM.Travel.Vehicle;
import PSM.Travel.VehicleType;
import PSM.UserManagement.User;
import PSM.UserManagement.UserNotification;
import PSM.UserManagement.api.user.UserNotificationRepository;
import PSM.UserManagement.api.user.UserRepository;
import PSM.UserManagement.notification.NotificationCacheService;
import PSM.UserManagement.notification.NotificationWebSocketService;

@Service
public class VehicleService {
    private final VehicleRepository repository;
    private final StopService stopService;
    private final UserRepository userRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationCacheService notificationCacheService;
    private final NotificationWebSocketService notificationWebSocketService;

    public VehicleService(VehicleRepository repository, StopService stopService, UserRepository userRepository, UserNotificationRepository userNotificationRepository, NotificationCacheService notificationCacheService, NotificationWebSocketService notificationWebSocketService) {
        this.repository = repository;
        this.stopService = stopService;
        this.userRepository = userRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.notificationCacheService = notificationCacheService;
        this.notificationWebSocketService = notificationWebSocketService;
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
    public Vehicle arrive(UUID vehicleId, UUID stopId, UUID routeId, String routeName, VehicleType vehicleType) {
        Vehicle vehicle = findById(vehicleId);
        Stop stop = stopService.findById(stopId);
        
        List<User> rawObservers = userRepository.findObserversByStop(stop);

        List<User> distinctObservers = rawObservers.stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(
                        User::getId, 
                        user -> user, 
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .toList();

        if (distinctObservers.isEmpty()) {
            return vehicle;
        }

        vehicle.setStop(stop);
        vehicle.setLocation(stop.getLocation());
        vehicle.arrived();
        repository.save(vehicle);

        String tipoTexto = "veículo";
        if (vehicleType != null) {
            tipoTexto = switch (vehicleType) {
                case BUS -> "bus";
                case METRO -> "metro";
                case TRAIN -> "train";
            };
        }

        String message = String.format(" The %s on line %s has arrived at the %s stop (%s).", 
                tipoTexto,
                (routeName != null ? routeName : "parceira"), 
                stop.getName(), stop.getStopCode());

        // 5. Criar e persistir notificações
        List<UserNotification> notificationsToSave = new ArrayList<>();
        for (User user : distinctObservers) {
            UserNotification notification = new UserNotification(stop, vehicleId, routeId, routeName, vehicleType, message);
            notification.setUser(user);
            notificationsToSave.add(notification);
        }

        // Gravação Blindada Antiduplicados
        if (!notificationsToSave.isEmpty()) {
            List<UserNotification> uniqueNotifications = notificationsToSave.stream()
                    .collect(Collectors.toMap(
                            n -> n.getUser().getId().toString() + "_" + n.getStopId() + "_" + n.getVehicleId(),
                            n -> n,
                            (n1, n2) -> n1
                    ))
                    .values()
                    .stream()
                    .toList();

            userNotificationRepository.saveAll(uniqueNotifications);
            
            // 6. NOVO: Enviar notificações em TEMPO REAL via WebSocket (push)
            // Isto faz com que o cliente receba a notificação imediatamente, sem polling
            for (UserNotification notification : uniqueNotifications) {
                notificationWebSocketService.notifyUser(notification.getUser().getId(), notification);
            }
        }

        // 7. Atualizar os utilizadores afetados e limpar a cache do Redis
        userRepository.saveAll(distinctObservers);
        distinctObservers.stream().map(User::getId).forEach(notificationCacheService::evict);
        
        return vehicle;
    }

    @Transactional
    public Vehicle arrive(UUID vehicleId, UUID stopId) {
        return this.arrive(vehicleId, stopId, null, null, null);
    }
}