package PSM.Travel.api.trip;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import PSM.Travel.Trip;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findByEndTimeIsNull();

    @EntityGraph(attributePaths = {"route", "route.schedules"})
    Optional<Trip> findById(UUID id);

    @Query("SELECT t FROM Trip t LEFT JOIN FETCH t.route WHERE t.endTime IS NULL")
    List<Trip> findActiveTripsWithRoute();

    @Query("SELECT t FROM Trip t WHERE t.route.id = :routeId AND t.endTime IS NULL ORDER BY t.startTime DESC")
    List<Trip> findActiveByRouteId(@Param("routeId") UUID routeId);
}
