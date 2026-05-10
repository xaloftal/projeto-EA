package PSM.Location.api.routestop;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import PSM.Location.RouteStop;

public interface RouteStopRepository extends JpaRepository<RouteStop, UUID> {
    List<RouteStop> findByRouteIdOrderBySequenceAsc(UUID routeId);
}
