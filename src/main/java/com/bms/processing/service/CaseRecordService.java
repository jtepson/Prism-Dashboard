package com.bms.processing.service;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.PatientStatus;
import com.bms.processing.model.ThirdPartyStatus;
import com.bms.processing.repository.CaseRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.management.Notification;

@Service
public class CaseRecordService {

    private final CaseRecordRepository repository;
    private final AuditEventService auditEventService;
    private final NotificationService notificationService;

    public CaseRecordService(
            CaseRecordRepository repository,
            AuditEventService auditEventService,
            NotificationService notificationService
    ) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.notificationService = notificationService;
    }

    public List<CaseRecordEntity> findAll() {
        return repository.findAll();
    }

    public CaseRecordEntity save(CaseRecordEntity record) {
        throw new InvalidWorkflowTransitionException(
                "Direct save is not allowed. Use explicit service methods."
        );
    }

    public boolean isReadyToFinalize(CaseRecordEntity record) {
        if (record == null) {
            return false;
        }

        if (record.getPatientStatus() != PatientStatus.PROCESSING) {
            return false;
        }

        boolean neuroReady =
                record.getNeuroreaderStatus() == ThirdPartyStatus.SENT
                        || record.getNeuroreaderStatus() == ThirdPartyStatus.ERROR;

        if (!neuroReady) {
            return false;
        }

        if (record.isMinorAtScan()) {
            return record.getDuramapStatus() == ThirdPartyStatus.SENT
                    || record.getDuramapStatus() == ThirdPartyStatus.ERROR
                    || record.getDuramapStatus() == ThirdPartyStatus.COMPLETED;
        }

        boolean imekaReady =
                record.getImekaStatus() == ThirdPartyStatus.UPLOADED
                        || record.getImekaStatus() == ThirdPartyStatus.ERROR;

        if (!imekaReady) {
            return false;
        }

        if (record.getImekaStatus() == ThirdPartyStatus.ERROR) {
            return record.getDuramapStatus() == ThirdPartyStatus.SENT
                    || record.getDuramapStatus() == ThirdPartyStatus.ERROR
                    || record.getDuramapStatus() == ThirdPartyStatus.COMPLETED;
        }

        return true;
    }

    public CaseRecordEntity finalizeCase(CaseRecordEntity record, boolean studyAvailableInBmsView) {
        if (record == null) {
            throw new InvalidWorkflowTransitionException("Case record was not provided.");
        }

        if (!studyAvailableInBmsView) {
            throw new InvalidWorkflowTransitionException("You must confirm the study is available in BMS View.");
        }

        if (!isReadyToFinalize(record)) {
            throw new InvalidWorkflowTransitionException("Case is not ready to finalize.");
        }

        boolean hasInternalErrors =
                record.getNotes() != null && !record.getNotes().trim().isEmpty();

        boolean hasThirdPartyErrors =
                record.getImekaStatus() == ThirdPartyStatus.ERROR
                        || record.getDuramapStatus() == ThirdPartyStatus.ERROR
                        || record.getNeuroreaderStatus() == ThirdPartyStatus.ERROR;

        PatientStatus oldStatus = record.getPatientStatus();

        if (hasThirdPartyErrors) {
            record.setPatientStatus(PatientStatus.PROCESSED_WITH_THIRD_PARTY_ERRORS);
        } else if (hasInternalErrors) {
            record.setPatientStatus(PatientStatus.PROCESSED_WITH_ERRORS);
        } else {
            record.setPatientStatus(PatientStatus.PROCESSED);
        }

        record.setProcessedDate(LocalDateTime.now());

        CaseRecordEntity savedRecord = repository.save(record);

        //audit event
        auditEventService.logEvent(
                savedRecord.getId(),
                "CASE_FINALIZED",
                "Case completed for " + savedRecord.getPatientLastName() + ", " + savedRecord.getPatientFirstName(),
                oldStatus.name(),
                savedRecord.getPatientStatus().name(),
                "SYSTEM"
        );

        //email trigger
        notificationService.notifyCaseFinalized(
                savedRecord.getPatientLastName()
                        + ", "
                        + savedRecord.getPatientFirstName()
        );

        return savedRecord;
    }

    public CaseRecordEntity markCaseAsError(CaseRecordEntity record, String errorExplanation) {
        if (record == null) {
            throw new InvalidWorkflowTransitionException("Case record was not provided.");
        }

        if (errorExplanation == null || errorExplanation.trim().isEmpty()) {
            throw new InvalidWorkflowTransitionException(
                    "An explanation is required before sending a study to Errors."
            );
        }

        PatientStatus oldStatus = record.getPatientStatus();

        record.setNotes(errorExplanation.trim());
        record.setPatientStatus(PatientStatus.ERROR);

        if (record.getPatientStatus() != PatientStatus.ACQUIRED
                && record.getPatientStatus() != PatientStatus.PROCESSING) {
            throw new InvalidWorkflowTransitionException(
                    "Only ACQUIRED or PROCESSING cases can be moved to Errors."
            );
        }

        CaseRecordEntity savedRecord = repository.save(record);

        //audit event
        auditEventService.logEvent(
                savedRecord.getId(),
                "CASE_ERROR",
                "Error reported for " + savedRecord.getPatientLastName() + ", " + savedRecord.getPatientFirstName(),
                oldStatus != null ? oldStatus.name() : null,
                savedRecord.getPatientStatus().name(),
                "SYSTEM"
        );

        //email trigger
        notificationService.notifyCaseError(
                savedRecord.getPatientLastName()
                        + ", "
                        + savedRecord.getPatientFirstName(),
                errorExplanation.trim()
        );

        return savedRecord;
    }

    public CaseRecordEntity updateImekaStatus(
            CaseRecordEntity record,
            ThirdPartyStatus status,
            String errorNote,
            LocalDate sentDate
    ) {
        validateRecord(record);
        validateStatus(status, "IMEKA");

        if (record.isMinorAtScan()) {
            throw new InvalidWorkflowTransitionException("IMEKA is not used for minor cases.");
        }

        if (status == ThirdPartyStatus.COMPLETED) {
            throw new InvalidWorkflowTransitionException("IMEKA uses UPLOADED instead of COMPLETED.");
        }

        if (status == ThirdPartyStatus.ERROR) {
            requireErrorNote(errorNote, "IMEKA");
            record.setImekaStatus(ThirdPartyStatus.ERROR);
            record.setImekaErrorNote(errorNote.trim());
        } else {
            record.setImekaStatus(status);
            record.setImekaErrorNote(null);
        }

        if (status == ThirdPartyStatus.NOT_SENT) {
            record.setImekaSentDate(null);
        } else if (status == ThirdPartyStatus.SENT) {
            if (sentDate == null) {
                throw new InvalidWorkflowTransitionException("IMEKA sent date is required.");
            }
            record.setImekaSentDate(sentDate);
        } else if (status == ThirdPartyStatus.UPLOADED) {
            if (sentDate != null) {
                record.setImekaSentDate(sentDate);
            }

            if (record.getImekaUploadedDate() == null) {
                record.setImekaUploadedDate(LocalDateTime.now());
            }
        }

        if (status != ThirdPartyStatus.ERROR) {
            record.setDuramapStatus(ThirdPartyStatus.NOT_SENT);
            record.setDuramapSentDate(null);
            record.setDuramapErrorNote(null);
        }

        return repository.save(record);
    }

    public CaseRecordEntity updateDuramapStatus(
            CaseRecordEntity record,
            ThirdPartyStatus status,
            String errorNote,
            LocalDate sentDate
    ) {
        validateRecord(record);
        validateStatus(status, "DuraMap");

        if (!record.isMinorAtScan() && record.getImekaStatus() != ThirdPartyStatus.ERROR) {
            throw new InvalidWorkflowTransitionException(
                    "DuraMap is only used for minors or when IMEKA is in ERROR."
            );
        }

        if (status == ThirdPartyStatus.UPLOADED) {
            throw new InvalidWorkflowTransitionException("DuraMap does not use UPLOADED.");
        }

        if (status == ThirdPartyStatus.ERROR) {
            requireErrorNote(errorNote, "DuraMap");
            record.setDuramapStatus(ThirdPartyStatus.ERROR);
            record.setDuramapErrorNote(errorNote.trim());
        } else {
            record.setDuramapStatus(status);
            record.setDuramapErrorNote(null);
        }

        if (status == ThirdPartyStatus.NOT_SENT) {
            record.setDuramapSentDate(null);
        } else if (status == ThirdPartyStatus.SENT || status == ThirdPartyStatus.COMPLETED) {
            if (sentDate == null) {
                throw new InvalidWorkflowTransitionException("DuraMap sent date is required.");
            }
            record.setDuramapSentDate(sentDate);
        }

        return repository.save(record);
    }

    public CaseRecordEntity updateNeuroreaderStatus(
            CaseRecordEntity record,
            ThirdPartyStatus status,
            String errorNote,
            LocalDate sentDate
    ) {
        validateRecord(record);
        validateStatus(status, "Neuroreader");

        if (status == ThirdPartyStatus.COMPLETED || status == ThirdPartyStatus.UPLOADED) {
            throw new InvalidWorkflowTransitionException("Neuroreader does not use COMPLETED or UPLOADED.");
        }

        if (status == ThirdPartyStatus.ERROR) {
            requireErrorNote(errorNote, "Neuroreader");
            record.setNeuroreaderStatus(ThirdPartyStatus.ERROR);
            record.setNeuroreaderErrorNote(errorNote.trim());
        } else {
            record.setNeuroreaderStatus(status);
            record.setNeuroreaderErrorNote(null);
        }

        if (status == ThirdPartyStatus.NOT_SENT) {
            record.setNeuroreaderSentDate(null);
        } else if (status == ThirdPartyStatus.SENT) {
            if (sentDate == null) {
                throw new InvalidWorkflowTransitionException("Neuroreader sent date is required.");
            }
            record.setNeuroreaderSentDate(sentDate);
        }

        return repository.save(record);
    }

    public CaseRecordEntity updateImekaSentDate(CaseRecordEntity record, LocalDate sentDate) {
        validateRecord(record);

        if (record.isMinorAtScan()) {
            throw new InvalidWorkflowTransitionException("IMEKA is not used for minor cases.");
        }

        if (record.getImekaStatus() == null || record.getImekaStatus() == ThirdPartyStatus.NOT_SENT) {
            throw new InvalidWorkflowTransitionException("Set an IMEKA status before assigning a sent date.");
        }

        record.setImekaSentDate(sentDate);
        return repository.save(record);
    }

    public CaseRecordEntity updateDuramapSentDate(CaseRecordEntity record, LocalDate sentDate) {
        validateRecord(record);

        if (!record.isMinorAtScan() && record.getImekaStatus() != ThirdPartyStatus.ERROR) {
            throw new InvalidWorkflowTransitionException(
                    "DuraMap sent date can only be edited for minors or IMEKA fallback cases."
            );
        }

        if (record.getDuramapStatus() == null || record.getDuramapStatus() == ThirdPartyStatus.NOT_SENT) {
            throw new InvalidWorkflowTransitionException("Set a DuraMap status before assigning a sent date.");
        }

        record.setDuramapSentDate(sentDate);
        return repository.save(record);
    }

    public CaseRecordEntity updateNeuroreaderSentDate(CaseRecordEntity record, LocalDate sentDate) {
        validateRecord(record);

        if (record.getNeuroreaderStatus() == null || record.getNeuroreaderStatus() == ThirdPartyStatus.NOT_SENT) {
            throw new InvalidWorkflowTransitionException("Set a Neuroreader status before assigning a sent date.");
        }

        record.setNeuroreaderSentDate(sentDate);
        return repository.save(record);
    }

    public CaseRecordEntity updatePatientStatus(CaseRecordEntity record, PatientStatus newStatus) {
        throw new InvalidWorkflowTransitionException(
                "Direct patient status updates are not allowed. Use an explicit workflow method."
        );
    }

    public CaseRecordEntity saveEditedCase(CaseRecordEntity record) {
        if (record == null) {
            throw new InvalidWorkflowTransitionException("Case record was not provided.");
        }

        if (record.getPatientStatus() == null) {
            throw new InvalidWorkflowTransitionException("Patient status is required.");
        }

        if (record.getImekaStatus() == ThirdPartyStatus.ERROR
                && (record.getImekaErrorNote() == null || record.getImekaErrorNote().trim().isEmpty())) {
            throw new InvalidWorkflowTransitionException("IMEKA error note is required.");
        }

        if (record.getDuramapStatus() == ThirdPartyStatus.ERROR
                && (record.getDuramapErrorNote() == null || record.getDuramapErrorNote().trim().isEmpty())) {
            throw new InvalidWorkflowTransitionException("DuraMap error note is required.");
        }

        if (record.getNeuroreaderStatus() == ThirdPartyStatus.ERROR
                && (record.getNeuroreaderErrorNote() == null || record.getNeuroreaderErrorNote().trim().isEmpty())) {
            throw new InvalidWorkflowTransitionException("Neuroreader error note is required.");
        }

        if (record.isMinorAtScan()) {
            record.setImekaStatus(ThirdPartyStatus.NOT_SENT);
            record.setImekaSentDate(null);
            record.setImekaErrorNote(null);

            record.setNeuroreaderStatus(ThirdPartyStatus.NOT_SENT);
            record.setNeuroreaderSentDate(null);
            record.setNeuroreaderErrorNote(null);
        } else {
            if (record.getImekaStatus() != ThirdPartyStatus.ERROR) {
                record.setDuramapStatus(ThirdPartyStatus.NOT_SENT);
                record.setDuramapSentDate(null);
                record.setDuramapErrorNote(null);
            }
        }

        if (!record.isMinorAtScan()) {
            if (record.getImekaStatus() == ThirdPartyStatus.COMPLETED) {
                throw new InvalidWorkflowTransitionException("IMEKA uses UPLOADED instead of COMPLETED.");
            }

            if (record.getNeuroreaderStatus() == ThirdPartyStatus.COMPLETED
                    || record.getNeuroreaderStatus() == ThirdPartyStatus.UPLOADED) {
                throw new InvalidWorkflowTransitionException("Neuroreader does not use COMPLETED or UPLOADED.");
            }

            if (record.getImekaStatus() != ThirdPartyStatus.ERROR
                    && record.getDuramapStatus() != null
                    && record.getDuramapStatus() != ThirdPartyStatus.NOT_SENT) {
                throw new InvalidWorkflowTransitionException("DuraMap is only used for adults when IMEKA is in ERROR.");
            }
        }

        if (record.getPatientStatus() == PatientStatus.ERROR) {
            if (record.getNotes() == null || record.getNotes().trim().isEmpty()) {
                throw new InvalidWorkflowTransitionException(
                        "Notes are required when saving a case in ERROR status."
                );
            }
        }

        if (record.getPatientStatus() == PatientStatus.PROCESSED_WITH_ERRORS
                || record.getPatientStatus() == PatientStatus.PROCESSED_WITH_THIRD_PARTY_ERRORS) {
            if (record.getNotes() == null || record.getNotes().trim().isEmpty()) {
                throw new InvalidWorkflowTransitionException(
                        "Notes are required for processed cases with errors."
                );
            }
        }

        if (record.getPatientStatus() == PatientStatus.PROCESSED
                || record.getPatientStatus() == PatientStatus.PROCESSED_WITH_ERRORS
                || record.getPatientStatus() == PatientStatus.PROCESSED_WITH_THIRD_PARTY_ERRORS) {

            if (!isReadyToFinalize(record)) {
                throw new InvalidWorkflowTransitionException("Case cannot be saved as processed because it is not ready to finalize.");
            }

            if (record.getProcessedDate() == null) {
                record.setProcessedDate(LocalDateTime.now());
            }
        }

        if (record.getImekaStatus() == null || record.getImekaStatus() == ThirdPartyStatus.NOT_SENT) {
            record.setImekaSentDate(null);
        }

        if (record.getDuramapStatus() == null || record.getDuramapStatus() == ThirdPartyStatus.NOT_SENT) {
            record.setDuramapSentDate(null);
        }

        if (record.getNeuroreaderStatus() == null || record.getNeuroreaderStatus() == ThirdPartyStatus.NOT_SENT) {
            record.setNeuroreaderSentDate(null);
        }

        if (record.getImekaStatus() == ThirdPartyStatus.SENT || record.getImekaStatus() == ThirdPartyStatus.UPLOADED) {
            if (record.getImekaSentDate() == null) {
                throw new InvalidWorkflowTransitionException("IMEKA sent date is required.");
            }
        }

        if (record.getDuramapStatus() == ThirdPartyStatus.SENT || record.getDuramapStatus() == ThirdPartyStatus.COMPLETED) {
            if (record.getDuramapSentDate() == null) {
                throw new InvalidWorkflowTransitionException("DuraMap sent date is required.");
            }
        }

        if (record.getNeuroreaderStatus() == ThirdPartyStatus.SENT) {
            if (record.getNeuroreaderSentDate() == null) {
                throw new InvalidWorkflowTransitionException("Neuroreader sent date is required.");
            }
        }

        return repository.save(record);
    }
    
    public CaseRecordEntity markImagesReceived(CaseRecordEntity record) {
        
        //saving pt status in db for audit logging purposes
        PatientStatus oldStatus = record.getPatientStatus();
        validateRecord(record);

        if (record.getPatientStatus() != PatientStatus.UPCOMING
                && record.getPatientStatus() != PatientStatus.VERIFYING) {
            throw new InvalidWorkflowTransitionException(
                    "Only UPCOMING or VERIFYING cases can be marked as received."
            );
        }

        record.setPatientStatus(PatientStatus.ACQUIRED);
        record.setImagesReceivedDate(LocalDate.now());

        // clear downstream timestamps in case this record was in a weird state
        record.setProcessedDate(null);
        record.setCompletedDate(null);

        // DuraMap always runs
        record.setDuramapStatus(ThirdPartyStatus.NOT_SENT);
        record.setDuramapSentDate(null);
        record.setDuramapErrorNote(null);

        if (record.isMinorAtScan()) {
            // minors do not use IMEKA
            record.setImekaStatus(ThirdPartyStatus.NOT_SENT);
            record.setImekaSentDate(null);
            record.setImekaErrorNote(null);

            record.setNeuroreaderStatus(ThirdPartyStatus.NOT_SENT);
            record.setNeuroreaderSentDate(null);
            record.setNeuroreaderErrorNote(null);
        } else {
            // adults run all third parties
            record.setImekaStatus(ThirdPartyStatus.NOT_SENT);
            record.setImekaSentDate(null);
            record.setImekaErrorNote(null);

            record.setNeuroreaderStatus(ThirdPartyStatus.NOT_SENT);
            record.setNeuroreaderSentDate(null);
            record.setNeuroreaderErrorNote(null);
        }

        CaseRecordEntity savedRecord = repository.save(record);

        auditEventService.logEvent(
                savedRecord.getId(),
                "IMAGES_RECEIVED",
                "Images received for " + savedRecord.getPatientLastName() + ", " + savedRecord.getPatientFirstName(),
                oldStatus.name(),
                savedRecord.getPatientStatus().name(),
                "SYSTEM"
        );

        return savedRecord;
    }

    public CaseRecordEntity startProcessing(CaseRecordEntity record) {
        validateRecord(record);

        if (record.getPatientStatus() != PatientStatus.ACQUIRED) {
            throw new InvalidWorkflowTransitionException(
                    "Only ACQUIRED cases can be started for processing."
            );
        }

        PatientStatus oldStatus = record.getPatientStatus();

        record.setPatientStatus(PatientStatus.PROCESSING);

        CaseRecordEntity savedRecord = repository.save(record);

        auditEventService.logEvent(
                savedRecord.getId(),
                "PROCESSING_STARTED",
                "Processing started for " + savedRecord.getPatientLastName() + ", " + savedRecord.getPatientFirstName(),
                oldStatus.name(),
                savedRecord.getPatientStatus().name(),
                "SYSTEM"
        );

        return savedRecord;
    }

    public CaseRecordEntity markCompleted(CaseRecordEntity record) {
        validateRecord(record);

        PatientStatus status = record.getPatientStatus();
        if (status != PatientStatus.PROCESSED
                && status != PatientStatus.PROCESSED_WITH_ERRORS
                && status != PatientStatus.PROCESSED_WITH_THIRD_PARTY_ERRORS) {
            throw new InvalidWorkflowTransitionException(
                    "Only processed cases can be marked completed."
            );
        }

        if (record.getProcessedDate() == null) {
            throw new InvalidWorkflowTransitionException(
                    "Processed date is required before marking a case completed."
            );
        }

        PatientStatus oldStatus = record.getPatientStatus();

        record.setPatientStatus(PatientStatus.COMPLETED);

        if (record.getCompletedDate() == null) {
            record.setCompletedDate(LocalDateTime.now());
        }

        CaseRecordEntity savedRecord = repository.save(record);

        //trigger for audit event log
        auditEventService.logEvent(
                savedRecord.getId(),
                "CASE_COMPLETED",
                "Case completed for " + savedRecord.getPatientLastName() + ", " + savedRecord.getPatientFirstName(),
                oldStatus.name(),
                savedRecord.getPatientStatus().name(),
                "SYSTEM"
        );
        
        //trigger to send email
        notificationService.notifyCaseCompleted(
                savedRecord.getPatientLastName()
                        + ", "
                        + savedRecord.getPatientFirstName()
        );

        return savedRecord;
    }

    public CaseRecordEntity updateInvoiceSent(CaseRecordEntity record, boolean invoiceSent) {
        validateRecord(record);

        PatientStatus status = record.getPatientStatus();
        if (status != PatientStatus.PROCESSED
                && status != PatientStatus.PROCESSED_WITH_ERRORS
                && status != PatientStatus.PROCESSED_WITH_THIRD_PARTY_ERRORS
                && status != PatientStatus.COMPLETED) {
            throw new InvalidWorkflowTransitionException(
                    "Invoice status can only be updated for processed or completed cases."
            );
        }

        record.setInvoiceSent(invoiceSent);
        return repository.save(record);
    }

    public CaseRecordEntity returnToProcessing(CaseRecordEntity record) {
        validateRecord(record);

        if (record.getPatientStatus() != PatientStatus.ERROR) {
            throw new InvalidWorkflowTransitionException(
                    "Only ERROR cases can be returned to processing."
            );
        }

        PatientStatus oldStatus = record.getPatientStatus();

        record.setPatientStatus(PatientStatus.PROCESSING);

        CaseRecordEntity savedRecord = repository.save(record);

        auditEventService.logEvent(
                savedRecord.getId(),
                "RETURNED_TO_PROCESSING",
                "Error resolved for " + savedRecord.getPatientLastName() + ", " + savedRecord.getPatientFirstName() + ", returned to Processing",
                oldStatus.name(),
                savedRecord.getPatientStatus().name(),
                "SYSTEM"
        );

        return savedRecord;
    }

    public CaseRecordEntity updateSummaryIdentityFields(
        CaseRecordEntity record,
        String patientLastName,
        String patientFirstName,
        String patientId,
        String siteName
    ) {
        validateRecord(record);

        record.setPatientLastName(trimToNull(patientLastName));
        record.setPatientFirstName(trimToNull(patientFirstName));
        record.setPatientId(trimToNull(patientId));
        record.setSiteName(trimToNull(siteName));

        return repository.save(record);
    }

    public CaseRecordEntity updateUpcomingCaseDetails(CaseRecordEntity record) {
        validateRecord(record);

        if (record.getPatientStatus() != PatientStatus.UPCOMING
                && record.getPatientStatus() != PatientStatus.VERIFYING) {
            throw new InvalidWorkflowTransitionException(
                    "Only UPCOMING or VERIFYING cases can be edited here."
            );
        }

        clearProcessingFields(record);
        normalizeThirdPartyDefaults(record);

        return repository.save(record);
    }

    public CaseRecordEntity createUpcomingCase(CaseRecordEntity record) {
        validateRecord(record);

        if (record.getPatientStatus() == null) {
            record.setPatientStatus(PatientStatus.UPCOMING);
        }

        if (record.getPatientStatus() != PatientStatus.UPCOMING
                && record.getPatientStatus() != PatientStatus.VERIFYING) {
            throw new InvalidWorkflowTransitionException(
                    "New cases must be UPCOMING or VERIFYING."
            );
        }

        clearProcessingFields(record);
        normalizeThirdPartyDefaults(record);

        CaseRecordEntity savedRecord = repository.save(record);

        //audit event
        auditEventService.logEvent(
                savedRecord.getId(),
                "PATIENT_CREATED",
                "Patient created: " + savedRecord.getPatientLastName() + ", " + savedRecord.getPatientFirstName(),
                null,
                savedRecord.getPatientLastName() + ", "
                        + savedRecord.getPatientFirstName(),
                "SYSTEM"
        );

        //email trigger
        notificationService.notifyPatientCreated(
                savedRecord.getPatientLastName()
                        + ", "
                        + savedRecord.getPatientFirstName()
        );

        return savedRecord;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateRecord(CaseRecordEntity record) {
        if (record == null) {
            throw new InvalidWorkflowTransitionException("Case record was not provided.");
        }
    }

    private void validateStatus(ThirdPartyStatus status, String label) {
        if (status == null) {
            throw new InvalidWorkflowTransitionException(label + " status is required.");
        }
    }

    private void requireErrorNote(String errorNote, String label) {
        if (errorNote == null || errorNote.trim().isEmpty()) {
            throw new InvalidWorkflowTransitionException(label + " error note is required.");
        }
    }

    private void clearProcessingFields(CaseRecordEntity record) {
        record.setImagesReceivedDate(null);
        record.setProcessedDate(null);
        record.setCompletedDate(null);
    }

    private void normalizeThirdPartyDefaults(CaseRecordEntity record) {
        record.setDuramapStatus(ThirdPartyStatus.NOT_SENT);
        record.setDuramapSentDate(null);
        record.setDuramapErrorNote(null);

        if (record.isMinorAtScan()) {
            record.setImekaStatus(ThirdPartyStatus.NOT_SENT);
            record.setImekaSentDate(null);
            record.setImekaErrorNote(null);

            record.setNeuroreaderStatus(ThirdPartyStatus.NOT_SENT);
            record.setNeuroreaderSentDate(null);
            record.setNeuroreaderErrorNote(null);
        } else {
            record.setImekaStatus(ThirdPartyStatus.NOT_SENT);
            record.setImekaSentDate(null);
            record.setImekaErrorNote(null);

            record.setNeuroreaderStatus(ThirdPartyStatus.NOT_SENT);
            record.setNeuroreaderSentDate(null);
            record.setNeuroreaderErrorNote(null);
        }
    }
}