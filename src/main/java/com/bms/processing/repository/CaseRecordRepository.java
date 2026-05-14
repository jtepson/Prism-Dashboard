package com.bms.processing.repository;

import com.bms.processing.entity.CaseRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRecordRepository extends JpaRepository<CaseRecordEntity, Long> {
}
