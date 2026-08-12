package com.bms.processing.repository;

import com.bms.processing.entity.CaseRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bms.processing.model.ThirdPartyStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface CaseRecordRepository extends JpaRepository<CaseRecordEntity, Long> {

    //adding in for issue 22 fix, for redundant studyuid checking and blocking repeated connections.
    boolean existsByStudyInstanceUidAndIdNot(
            String studyInstanceUid,
            Long id
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CaseRecordEntity c
            set c.imekaStatus = :status,
                c.imekaSentDate = :sentDate,
                c.imekaUploadedDate = :uploadedDate
            where c.id = :caseRecordId
            """)

    int markImekaUploadedFromReport(
            @Param("caseRecordId") Long caseRecordId,
            @Param("status") ThirdPartyStatus status,
            @Param("sentDate") LocalDate sentDate,
            @Param("uploadedDate") LocalDateTime uploadedDate
    );
}
