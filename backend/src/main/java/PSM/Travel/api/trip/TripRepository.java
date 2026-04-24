package PSM.Travel.api.trip;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Travel.Trip;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
}
