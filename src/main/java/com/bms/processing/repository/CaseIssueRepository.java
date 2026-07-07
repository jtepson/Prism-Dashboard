package com.bms.processing.repository;

import com.bms.processing.entity.CaseIssueEntity;
import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.CaseIssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseIssueRepository extends JpaRepository<CaseIssueEntity, Long> {

    List<CaseIssueEntity> findByCaseRecord(CaseRecordEntity caseRecord);

    List<CaseIssueEntity> findByStatus(CaseIssueStatus status);

    List<CaseIssueEntity> findByCaseRecordAndStatus(
            CaseRecordEntity caseRecord,
            CaseIssueStatus status
    );
}