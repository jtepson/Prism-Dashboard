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

    //db backed auditing for user activity for patient sensitive stuff - 7012026
    public void logCaseAction(
            String eventType,
            com.bms.processing.entity.CaseRecordEntity caseRecord,
            String targetName,
            String oldValue,
            String newValue,
            String createdBy
    ) {
        if (caseRecord == null) {
            return;
        }

        String patientInitials = buildPatientInitials(caseRecord);
        String patientId = caseRecord.getPatientId() == null
                ? "UNKNOWN"
                : caseRecord.getPatientId();

        String message = createdBy
                + " - "
                + eventType
                + " - "
                + patientInitials
                + " "
                + patientId;

        if (targetName != null && !targetName.isBlank()) {
            message += " - " + targetName;
        }

        if (oldValue != null && newValue != null) {
            message += " - " + oldValue + " to " + newValue;
        }

        logEvent(
                caseRecord.getId(),
                eventType,
                message,
                oldValue,
                newValue,
                createdBy
        );
    }

    private String buildPatientInitials(
            com.bms.processing.entity.CaseRecordEntity caseRecord
    ) {
        String firstInitial = caseRecord.getPatientFirstName() != null
                && !caseRecord.getPatientFirstName().isBlank()
                ? caseRecord.getPatientFirstName().substring(0, 1).toUpperCase()
                : "?";

        String lastInitial = caseRecord.getPatientLastName() != null
                && !caseRecord.getPatientLastName().isBlank()
                ? caseRecord.getPatientLastName().substring(0, 1).toUpperCase()
                : "?";

        return firstInitial + "." + lastInitial + ".";
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