package PSM.Location.api.stop;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Location.Stop;

@Repository
public interface StopRepository extends JpaRepository<Stop, UUID> {

	@EntityGraph(attributePaths = "location")
	List<Stop> findAll();

	@EntityGraph(attributePaths = "location")
	Optional<Stop> findById(UUID id);

	@EntityGraph(attributePaths = "location")
	Optional<Stop> findByStopCode(String stopCode);
}
