package PSM.Location.api.route;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Location.Route;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {
}
