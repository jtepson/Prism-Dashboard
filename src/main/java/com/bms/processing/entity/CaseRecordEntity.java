package com.bms.processing.entity;

import com.bms.processing.model.PatientStatus;
import com.bms.processing.model.ThirdPartyStatus;
import com.bms.processing.model.FinalOutcome;
import com.bms.processing.model.ProcessingOutcome;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "case_record")
public class CaseRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientLastName;

    @Column(name = "patient_first_name")
    private String patientFirstName;

    private String patientId;

    private String siteName;
        @Column(name = "owner_group")
        private String ownerGroup;

        @Column(name = "assigned_to_user")
        private String assignedToUser;

    @Enumerated(EnumType.STRING)
    private PatientStatus patientStatus;

    private LocalDate imagesReceivedDate;

    @Enumerated(EnumType.STRING)
    private ThirdPartyStatus imekaStatus;

    private LocalDate imekaSentDate;

    @Column(name = "imeka_uploaded_date")
    private LocalDateTime imekaUploadedDate;

    @Enumerated(EnumType.STRING)
    private ThirdPartyStatus duramapStatus;

    private LocalDate duramapSentDate;

    @Enumerated(EnumType.STRING)
    private ThirdPartyStatus neuroreaderStatus;

    private LocalDate neuroreaderSentDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "final_workflow_notes", columnDefinition = "TEXT")
    private String finalWorkflowNotes;

    @Column(columnDefinition = "TEXT")
    private String imekaErrorNote;

    @Column(columnDefinition = "TEXT")
    private String duramapErrorNote;

    @Column(columnDefinition = "TEXT")
    private String neuroreaderErrorNote;

    @Enumerated(EnumType.STRING)
    private FinalOutcome finalOutcome;

    @Enumerated(EnumType.STRING)
    private ProcessingOutcome processingOutcome;

    private LocalDateTime completedDate;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "sex")
    private String sex;

    @Column(name = "date_scanned")
    private LocalDate dateScanned;

    //BMS related information
    @Column(name = "funder")
    private String funder;

    @Column(name = "intake_sheet_done")
    private Boolean intakeSheetDone;

    @Column(name = "intake_sheet_sent")
    private Boolean intakeSheetSent;

    @Column(name = "invoice_sent_date")
    private LocalDate invoiceSentDate;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    //DICOM related stuff
    @Column(name = "study_instance_uid")
    private String studyInstanceUid;

    @Column(name = "accession_number")
    private String accessionNumber;

    @Column(name = "dicom_linked")
    private Boolean dicomLinked = false;

    public Long getId() {
        return id;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public void setPatientLastName(String patientLastName) {
        this.patientLastName = patientLastName;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    public void setPatientFirstName(String patientFirstName) {
        this.patientFirstName = patientFirstName;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public PatientStatus getPatientStatus() {
        return patientStatus;
    }

    public void setPatientStatus(PatientStatus patientStatus) {
        this.patientStatus = patientStatus;
    }

    public ThirdPartyStatus getImekaStatus() {
        return imekaStatus;
    }

    public void setImekaStatus(ThirdPartyStatus imekaStatus) {
        this.imekaStatus = imekaStatus;
    }

    public LocalDate getImekaSentDate() {
        return imekaSentDate;
    }

    public void setImekaSentDate(LocalDate imekaSentDate) {
        this.imekaSentDate = imekaSentDate;
    }

    public ThirdPartyStatus getDuramapStatus() {
        return duramapStatus;
    }

    public void setDuramapStatus(ThirdPartyStatus duramapStatus) {
        this.duramapStatus = duramapStatus;
    }

    public LocalDate getDuramapSentDate() {
        return duramapSentDate;
    }

    public void setDuramapSentDate(LocalDate duramapSentDate) {
        this.duramapSentDate = duramapSentDate;
    }

    public ThirdPartyStatus getNeuroreaderStatus() {
        return neuroreaderStatus;
    }

    public void setNeuroreaderStatus(ThirdPartyStatus neuroreaderStatus) {
        this.neuroreaderStatus = neuroreaderStatus;
    }

    public LocalDate getNeuroreaderSentDate() {
        return neuroreaderSentDate;
    }

    public void setNeuroreaderSentDate(LocalDate neuroreaderSentDate) {
        this.neuroreaderSentDate = neuroreaderSentDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public FinalOutcome getFinalOutcome() {
        return finalOutcome;
    }

    public void setFinalOutcome(FinalOutcome finalOutcome) {
        this.finalOutcome = finalOutcome;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public ProcessingOutcome getProcessingOutcome() {
    	return processingOutcome;
    }

    public void setProcessingOutcome(ProcessingOutcome processingOutcome) {
    	this.processingOutcome = processingOutcome;
    }

    public String getImekaErrorNote() {
        return imekaErrorNote;
    }

    public void setImekaErrorNote(String imekaErrorNote) {
        this.imekaErrorNote = imekaErrorNote;
    }

    public String getDuramapErrorNote() {
        return duramapErrorNote;
    }

    public void setDuramapErrorNote(String duramapErrorNote) {
        this.duramapErrorNote = duramapErrorNote;
    }

    public String getNeuroreaderErrorNote() {
        return neuroreaderErrorNote;
    }

    public void setNeuroreaderErrorNote(String neuroreaderErrorNote) {
        this.neuroreaderErrorNote = neuroreaderErrorNote;
    }

    public LocalDate getImagesReceivedDate() {
    	return imagesReceivedDate;
    }

    public void setImagesReceivedDate(LocalDate imagesReceivedDate) {
        this.imagesReceivedDate = imagesReceivedDate;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
    
    public LocalDate getDateScanned() {
        return dateScanned;
    }
    
    public void setDateScanned(LocalDate dateScanned) {
        this.dateScanned = dateScanned;
    }

    public String getFunder() {
        return funder;
    }
    
    public void setFunder(String funder) {
        this.funder = funder;
    }
    
    public Boolean getIntakeSheetDone() {
        return intakeSheetDone;
    }
    
    public void setIntakeSheetDone(Boolean intakeSheetDone) {
        this.intakeSheetDone = intakeSheetDone;
    }
    
    public Boolean getIntakeSheetSent() {
        return intakeSheetSent;
    }
    
    public void setIntakeSheetSent(Boolean intakeSheetSent) {
        this.intakeSheetSent = intakeSheetSent;
    }
    
    public LocalDate getInvoiceSentDate() {
        return invoiceSentDate;
    }
    
    public void setInvoiceSentDate(LocalDate invoiceSentDate) {
        this.invoiceSentDate = invoiceSentDate;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }
    
    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public String getImekaDuraPath() {
        if (imekaStatus == ThirdPartyStatus.ERROR && duramapStatus == ThirdPartyStatus.ERROR) {
            return "IMEKA / DuraMap Error";
        }
        if (imekaStatus == ThirdPartyStatus.ERROR) {
            return "DuraMap";
        }
        return "IMEKA";
    }

    public ThirdPartyStatus getActiveThirdPartyStatus() {
        if (imekaStatus == ThirdPartyStatus.ERROR) {
            return duramapStatus;
        }
        return imekaStatus;
    }

    public LocalDateTime getImekaUploadedDate() {
        return imekaUploadedDate;
    }

    public void setImekaUploadedDate(LocalDateTime imekaUploadedDate) {
        this.imekaUploadedDate = imekaUploadedDate;
    }

    public LocalDate getActiveThirdPartySentDate() {
        if (imekaStatus == ThirdPartyStatus.ERROR) {
            return duramapSentDate;
        }
        return imekaSentDate;
    }

    public boolean isDuramapAvailable() {
        return imekaStatus == ThirdPartyStatus.ERROR;
    }

    public boolean isMinorAtScan() {

    if (dateOfBirth == null) {
        return false;
    }

    //using this as an age calculation if data scanned isn't supplied, still offers only duramap as an option to minors at time of scan/entry.
    LocalDate referenceDate = dateScanned != null
            ? dateScanned
            : LocalDate.now();

    return Period.between(dateOfBirth, referenceDate).getYears() < 16;
    }

    public String getOwnerGroup() {
    return ownerGroup;
    }

    public void setOwnerGroup(String ownerGroup) {
        this.ownerGroup = ownerGroup;
    }

    public String getAssignedToUser() {
        return assignedToUser;
    }

    public void setAssignedToUser(String assignedToUser) {
        this.assignedToUser = assignedToUser;
    }

    public String getFinalWorkflowNotes() {
    return finalWorkflowNotes;
    }

    public void setFinalWorkflowNotes(String finalWorkflowNotes) {
        this.finalWorkflowNotes = finalWorkflowNotes;
    }

    //DICOM getters and setters yuh yuh
    public String getStudyInstanceUid() {
        return studyInstanceUid;
    }

    public void setStudyInstanceUid(String studyInstanceUid) {
        this.studyInstanceUid = studyInstanceUid;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public Boolean getDicomLinked() {
        return dicomLinked;
    }

    public void setDicomLinked(Boolean dicomLinked) {
        this.dicomLinked = dicomLinked;
    }
}
