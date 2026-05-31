package PSM.Travel.api.trip;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Travel.Trip;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    @EntityGraph(attributePaths = {"route", "route.schedules"})
    Optional<Trip> findById(UUID id);
}
