package com.bms.processing.service;

import com.bms.processing.entity.AuditEventEntity;
import com.bms.processing.repository.AuditEventRepository;
import com.bms.processing.entity.CaseRecordEntity;
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

    //updated 7012026 with timeline event for audit types
    public void logEvent(
            Long caseRecordId,
            String eventType,
            String message,
            String oldValue,
            String newValue,
            String createdBy,
            Boolean timelineEvent
    ) {

        AuditEventEntity event = new AuditEventEntity();

        event.setCaseRecordId(caseRecordId);
        event.setEventType(eventType);
        event.setMessage(message);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setCreatedBy(createdBy);
        event.setCreatedAt(LocalDateTime.now());

        event.setTimelineEvent(timelineEvent);

        auditEventRepository.save(event);
    }

    //db backed auditing for user activity for patient sensitive stuff - 7012026
    public void logCaseAction(
            String eventType,
            CaseRecordEntity caseRecord,
            String targetName,
            String oldValue,
            String newValue,
            String createdBy,
            Boolean timelineEvent
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
                createdBy,
                timelineEvent
        );
    }

    //admin specific auditing for review - 7012026
    public void logAdminAuditEvent(
            String eventType,
            CaseRecordEntity caseRecord,
            String targetName,
            String oldValue,
            String newValue,
            String createdBy
    ) {
        logCaseAction(
                eventType,
                caseRecord,
                targetName,
                oldValue,
                newValue,
                createdBy,
                false
        );
    }

    //general timeline - 7012026
    public void logTimelineEvent(
            String eventType,
            CaseRecordEntity caseRecord,
            String targetName,
            String oldValue,
            String newValue,
            String createdBy
    ) {
        logCaseAction(
                eventType,
                caseRecord,
                targetName,
                oldValue,
                newValue,
                createdBy,
                true
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
        return auditEventRepository
                .findByCaseRecordIdAndTimelineEventTrueOrderByCreatedAtDesc(caseRecordId);
    }

    public List<AuditEventEntity> getRecentAuditEvents() {
        return auditEventRepository
                .findTop500ByOrderByCreatedAtDesc();
    }
}