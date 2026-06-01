package PSM.UserManagement.api.user;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.UserManagement.UserNotification;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

	long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
