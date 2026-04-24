package PSM.ValidationManager.api.validationrecord;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.ValidationManager.ValidationRecord;

@Repository
public interface ValidationRecordRepository extends JpaRepository<ValidationRecord, UUID> {
}
