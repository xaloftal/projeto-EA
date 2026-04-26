package PSM.UserManagement.api.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import PSM.UserManagement.User;
import jakarta.persistence.LockModeType;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<User> findWithBalanceLockById(UUID id);

	boolean existsByEmail(String email);
}
