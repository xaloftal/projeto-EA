package PSM.Location.api.stopschedule;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Location.StopSchedule;

@Repository
public interface StopScheduleRepository extends JpaRepository<StopSchedule, UUID> {
}
