package com.bms.processing.repository;

import com.bms.processing.entity.CaseIssueEntity;
import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.CaseIssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CaseIssueRepository extends JpaRepository<CaseIssueEntity, Long> {

    List<CaseIssueEntity> findByCaseRecord(CaseRecordEntity caseRecord);

    List<CaseIssueEntity> findByStatus(CaseIssueStatus status);

    List<CaseIssueEntity> findByCaseRecordAndStatus(
            CaseRecordEntity caseRecord,
            CaseIssueStatus status
    );
    
    //adding this to fix this fetch-join query issue for a "lazy proxy" apparently? Learn something new everyday, should fix the loading issue
    //between upcoming issues and error page - updated 08172026
    @Query("""
        select i
        from CaseIssueEntity i
        join fetch i.caseRecord
        where i.status = :status
        """)
    List<CaseIssueEntity> findByStatusWithCaseRecord(
            @Param("status") CaseIssueStatus status
    );
}