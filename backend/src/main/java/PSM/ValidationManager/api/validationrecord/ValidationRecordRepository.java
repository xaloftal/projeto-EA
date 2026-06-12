package PSM.ValidationManager.api.validationrecord;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.ValidationManager.ValidationRecord;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import PSM.Ticketing.Title;
import PSM.ValidationManager.ValidationRecord;

@Repository
public interface ValidationRecordRepository extends JpaRepository<ValidationRecord, UUID> {
    @Query("SELECT vr FROM ValidationRecord vr JOIN vr.titles t WHERE t = :title AND vr.result = true ORDER BY vr.timestamp DESC")
    List<ValidationRecord> findSuccessfulValidationsByTitle(@Param("title") Title title);
}
