package com.bms.processing.repository;

import com.bms.processing.entity.CaseIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseIssueRepository extends JpaRepository<CaseIssueEntity, Long> {
}