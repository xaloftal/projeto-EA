package PSM.Location.api.route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import PSM.Location.Route;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

	@EntityGraph(attributePaths = { "routeStops", "routeStops.stop", "routeStops.stop.location", "schedules", "schedules.stop", "schedules.stop.location" })
	List<Route> findAll();

	@EntityGraph(attributePaths = { "routeStops", "routeStops.stop", "routeStops.stop.location", "schedules", "schedules.stop", "schedules.stop.location" })
	Optional<Route> findById(UUID id);

	@Query("SELECT r FROM Route r LEFT JOIN FETCH r.schedules s LEFT JOIN FETCH s.stop st LEFT JOIN FETCH st.location")
	List<Route> findAllWithSchedules();

	@Query("SELECT r FROM Route r LEFT JOIN FETCH r.schedules s LEFT JOIN FETCH s.stop st LEFT JOIN FETCH st.location WHERE r.id = :routeId")
	Optional<Route> findByIdWithSchedules(UUID routeId);

	@Query("SELECT DISTINCT r FROM Route r JOIN FETCH r.schedules s JOIN FETCH s.stop st LEFT JOIN FETCH st.location WHERE st.id = :stopId")
	List<Route> findAllWithSchedulesByStopId(UUID stopId);

	@Query("SELECT DISTINCT r FROM Route r " +
           "LEFT JOIN FETCH r.routeStops rs " +
           "LEFT JOIN FETCH rs.stop st " +
           "ORDER BY r.name ASC, rs.sequence ASC")
    List<Route> findAllWithRouteStops();
}

