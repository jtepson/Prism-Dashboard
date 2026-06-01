package com.bms.processing.service;

import com.bms.processing.entity.AuditEventEntity;
import com.bms.processing.repository.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(
            AuditEventRepository auditEventRepository
    ) {
        this.auditEventRepository = auditEventRepository;
    }

    public void logEvent(
            Long caseRecordId,
            String eventType,
            String message,
            String oldValue,
            String newValue,
            String createdBy
    ) {

        AuditEventEntity event = new AuditEventEntity();

        event.setCaseRecordId(caseRecordId);
        event.setEventType(eventType);
        event.setMessage(message);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setCreatedBy(createdBy);
        event.setCreatedAt(LocalDateTime.now());

        auditEventRepository.save(event);
    }

    public List<AuditEventEntity> getRecentEvents() {
        return auditEventRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public List<AuditEventEntity> getCaseEvents(Long caseRecordId) {
        return auditEventRepository.findByCaseRecordIdOrderByCreatedAtDesc(
                caseRecordId
        );
    }
}