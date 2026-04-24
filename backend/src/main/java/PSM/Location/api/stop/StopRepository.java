package PSM.Location.api.stop;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Location.Stop;

@Repository
public interface StopRepository extends JpaRepository<Stop, UUID> {
}
