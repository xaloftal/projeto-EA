package PSM.Location.api.stopschedule;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import PSM.Location.StopSchedule;

@Repository
public interface StopScheduleRepository extends JpaRepository<StopSchedule, UUID> {

	@Query("""
			SELECT DISTINCT ss
			FROM StopSchedule ss
			JOIN FETCH ss.route r
			JOIN FETCH ss.stop s
			LEFT JOIN FETCH s.location
			WHERE s.id IN :stopIds
			""")
	List<StopSchedule> findAllByStopIdInWithRoute(@Param("stopIds") Collection<UUID> stopIds);
}
