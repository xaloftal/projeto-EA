package PSM.Location.api.route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Location.Route;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

	@EntityGraph(attributePaths = { "schedules", "schedules.stop", "schedules.stop.location" })
	List<Route> findAll();

	@EntityGraph(attributePaths = { "schedules", "schedules.stop", "schedules.stop.location" })
	Optional<Route> findById(UUID id);
}
