package PSM.ExitManager.api.exitrecord;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.ExitManager.ExitRecord;

@Repository
public interface ExitRecordRepository extends JpaRepository<ExitRecord, UUID> {
}
