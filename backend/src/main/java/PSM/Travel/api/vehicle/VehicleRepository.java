package PSM.Travel.api.vehicle;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Travel.Vehicle;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
}
