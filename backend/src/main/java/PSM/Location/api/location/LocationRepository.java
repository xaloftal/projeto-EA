package PSM.Location.api.location;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Location.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
}
