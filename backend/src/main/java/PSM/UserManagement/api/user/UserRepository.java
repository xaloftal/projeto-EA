package PSM.UserManagement.api.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import PSM.Location.Stop;
import PSM.UserManagement.User;
import jakarta.persistence.LockModeType;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);

	@EntityGraph(attributePaths = { "notifications" })
	Optional<User> findWithNotificationsById(UUID id);

	@Query("select distinct u from User u join u.poi p where p = :stop")
	java.util.List<User> findObserversByStop(Stop stop);

	@EntityGraph(attributePaths = { "notifications" })
	@Query("select distinct u from User u join u.poi p where p = :stop")
	java.util.List<User> findObserversWithNotificationsByStop(Stop stop);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<User> findWithBalanceLockById(UUID id);

	@EntityGraph(attributePaths = { "card", "tickets", "notifications" })
	Optional<User> findWithDetailsById(UUID id);

	boolean existsByEmail(String email);
}
