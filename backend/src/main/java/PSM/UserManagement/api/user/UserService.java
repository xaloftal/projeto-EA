package PSM.UserManagement.api.user;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PSM.Location.Stop;
import PSM.Ticketing.Card;
import PSM.UserManagement.User;
import PSM.UserManagement.UserNotification;
import PSM.UserManagement.notification.NotificationCacheService;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository repository;
    private final NotificationCacheService notificationCacheService;

    public UserService(UserRepository repository, NotificationCacheService notificationCacheService) {
        this.repository = repository;
        this.notificationCacheService = notificationCacheService;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        logger.debug("Fetching all users");
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        logger.debug("Fetching user by id: {}", id);
        return repository.findWithDetailsById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<UserNotification> findNotifications(UUID id) {
        logger.debug("Fetching notifications for user {}", id);
        Optional<List<UserNotification>> cached = notificationCacheService.get(id);
        if (cached.isPresent()) {
            return cached.get();
        }
        List<UserNotification> notifications = repository.findWithNotificationsById(id)
                .map(User::getNotifications)
                .orElseGet(Collections::emptyList);
        notificationCacheService.put(id, notifications);
        return notifications;
    }

    @Transactional
    public void deleteNotification(UUID userId, UUID notificationId) {
        logger.debug("Deleting notification {} for user {}", notificationId, userId);
        User user = repository.findWithNotificationsById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean removed = user.getNotifications()
                .removeIf(notification -> Objects.equals(notification.getId(), notificationId));
        if (!removed) {
            throw new RuntimeException("Notification not found");
        }
        repository.save(user);
        notificationCacheService.evict(userId);
    }

    @Transactional
    public User create(User entity) {
        logger.debug("Creating user: {}", entity);
        return repository.save(entity);
    }

    @Transactional
    public User update(UUID id, User entity) {
        logger.debug("Updating user {} with data {}", id, entity);
        findById(id);
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<Stop> getUserPOI(UUID userId) {
        logger.debug("Fetching POI for user {}", userId);
        User user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getPOI();
    }

    @Transactional
    public void delete(UUID id) {
        logger.debug("Deleting user {}", id);
        repository.deleteById(id);
    }

    @Transactional
    public User assignCard(UUID userId, Card card) {
        logger.debug("Assigning card {} to user {}", card, userId);
        User user = findById(userId);
        user.setCard(card);
        return repository.save(user);
    }
}
