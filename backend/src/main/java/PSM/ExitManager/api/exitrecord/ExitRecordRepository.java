package PSM.ExitManager.api.exitrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import PSM.ExitManager.ExitRecord;

@Repository
public interface ExitRecordRepository extends JpaRepository<ExitRecord, UUID> {

    @Query("""
        SELECT DISTINCT er FROM ExitRecord er 
        JOIN FETCH er.titles t 
        LEFT JOIN FETCH t.fromStop fs
        LEFT JOIN FETCH t.toStop ts
        LEFT JOIN FETCH er.stop s
        WHERE t.user.id = :userId 
        ORDER BY er.timestamp DESC
    """)
    List<ExitRecord> findHistoryByUserId(@Param("userId") UUID userId);
}