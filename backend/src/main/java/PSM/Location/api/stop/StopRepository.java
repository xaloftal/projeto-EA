package PSM.Location.api.stop;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Location.Stop;
import PSM.Travel.VehicleType;

@Repository
public interface StopRepository extends JpaRepository<Stop, UUID> {

	@EntityGraph(attributePaths = "location")
	List<Stop> findAll();

	@EntityGraph(attributePaths = "location")
	Optional<Stop> findById(UUID id);

	@EntityGraph(attributePaths = "location")
	Optional<Stop> findByStopCode(String stopCode);

	@EntityGraph(attributePaths = "location")
	Optional<Stop> findFirstByStopCodeStartingWith(String stopCodePrefix);

	@EntityGraph(attributePaths = "location")
	Optional<Stop> findFirstByNameIgnoreCase(String name);

	@EntityGraph(attributePaths = "location")
	Optional<Stop> findByStopCodeAndStopType(String stopCode, VehicleType stopType);

	@EntityGraph(attributePaths = "location")
	Optional<Stop> findFirstByStopCodeStartingWithAndStopType(String stopCodePrefix, VehicleType stopType);

	@EntityGraph(attributePaths = "location")
	Optional<Stop> findFirstByNameIgnoreCaseAndStopType(String name, VehicleType stopType);

	@EntityGraph(attributePaths = "location")
	Optional<Stop> findFirstByNameContainingIgnoreCaseAndStopType(String namePart, VehicleType stopType);
}
