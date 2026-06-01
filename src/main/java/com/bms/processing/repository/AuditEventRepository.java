package com.bms.processing.repository;

import com.bms.processing.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository
        extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findTop50ByOrderByCreatedAtDesc();

    List<AuditEventEntity> findByCaseRecordIdOrderByCreatedAtDesc(Long caseRecordId);
}