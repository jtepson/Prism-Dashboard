package com.bms.processing.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CaseRecord {

    private String patientLastName;
    private String patientFirstInitial;
    private String patientId;
    private String siteName;

    private PatientStatus patientStatus;

    private ThirdPartyStatus imekaStatus;
    private LocalDate imekaSentDate;

    private ThirdPartyStatus duramapStatus;
    private LocalDate duramapSentDate;

    private ThirdPartyStatus neuroreaderStatus;
    private LocalDate neuroreaderSentDate;

    private String notes;

    private FinalOutcome finalOutcome;
    private LocalDateTime completedDate;

    public String getImekaDuraPath() {
        if (imekaStatus == ThirdPartyStatus.ERROR && duramapStatus == ThirdPartyStatus.ERROR) {
            return "IMEKA / DuraMap Error";
        }
        if (imekaStatus == ThirdPartyStatus.ERROR) {
            return "DuraMap";
        }
        return "IMEKA";
    }

    public boolean isDuramapAvailable() {
        return imekaStatus == ThirdPartyStatus.ERROR;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public void setPatientLastName(String patientLastName) {
        this.patientLastName = patientLastName;
    }

    public String getPatientFirstInitial() {
        return patientFirstInitial;
    }

    public void setPatientFirstInitial(String patientFirstInitial) {
        this.patientFirstInitial = patientFirstInitial;
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
    public ThirdPartyStatus getActiveThirdPartyStatus() {
        if (imekaStatus == ThirdPartyStatus.ERROR) {
            return duramapStatus;
        }
        return imekaStatus;
    }

    public LocalDate getActiveThirdPartySentDate() {
        if (imekaStatus == ThirdPartyStatus.ERROR) {
            return duramapSentDate;
        }
        return imekaSentDate;
    }
}
