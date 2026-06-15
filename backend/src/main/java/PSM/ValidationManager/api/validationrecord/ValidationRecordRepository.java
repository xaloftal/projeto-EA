package PSM.ValidationManager.api.validationrecord;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import PSM.ValidationManager.ValidationRecord;
import java.util.List;

@Repository
public interface ValidationRecordRepository extends JpaRepository<ValidationRecord, UUID> {
    
    @Query(value = "SELECT vr.* FROM catchit.validationrecord vr " +
           "INNER JOIN catchit.trip t ON vr.trip_id = t.id " +
           "WHERE t.vehicle_id = :vehicleId " +
           "AND vr.timestamp >= :start " +
           "AND vr.timestamp <= :end", 
           nativeQuery = true)
    List<ValidationRecord> findByVehicleAndPeriod(
        @Param("vehicleId") UUID vehicleId,
        @Param("start") java.time.LocalDateTime start,
        @Param("end") java.time.LocalDateTime end
    );
    
    @Query("SELECT vr FROM ValidationRecord vr JOIN vr.titles t WHERE t = :title AND vr.result = true ORDER BY vr.timestamp DESC")
    List<ValidationRecord> findSuccessfulValidationsByTitle(@Param("title") PSM.Ticketing.Title title);
}