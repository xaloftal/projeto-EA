package PSM.Location.api.zone;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Location.Zone;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {
}
