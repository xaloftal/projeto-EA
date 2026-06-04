package PSM.UserManagement.api.user;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PSM.Location.Stop;
import PSM.Ticketing.Card;
import PSM.UserManagement.User;
import PSM.UserManagement.UserNotification;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping(value = { "", "/" })
    public List<User> getAll() {
        logger.debug("Fetching all users");
        return service.findAll();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable UUID id) {
        logger.debug("Fetching user by id: {}", id);
        return service.findById(id);
    }

    @PostMapping
    public User create(@RequestBody User entity) {
        logger.debug("Creating user: {}", entity);
        return service.create(entity);
    }

    @PostMapping("/{id}/card")
    public User assignCard(@PathVariable UUID id, @RequestBody Card card) {
        logger.debug("Assigning card {} to user {}", card, id);
        return service.assignCard(id, card);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable UUID id, @RequestBody User entity) {
        logger.debug("Updating user {} with data {}", id, entity);
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        logger.debug("Deleting user {}", id);
        service.delete(id);
    }

    @GetMapping("/{id}/notifications")
    public Set<UserNotification> getNotifications(@PathVariable UUID id) {
        logger.debug("Fetching notifications for user {}", id);
        return service.findNotifications(id);
    }

    @DeleteMapping("/{id}/notifications/{notificationId}")
    public void deleteNotification(@PathVariable UUID id, @PathVariable UUID notificationId) {
        logger.debug("Deleting notification {} for user {}", notificationId, id);
        service.deleteNotification(id, notificationId);
    }

    @GetMapping("/{id}/poi")
    public Set<Stop> getPOI(@PathVariable UUID id) {
        logger.debug("Fetching POI for user {}", id);
        return service.getUserPOI(id);
    }
}
