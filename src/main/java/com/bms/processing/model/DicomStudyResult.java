package com.bms.processing.model;

public class DicomStudyResult {

    private String patientName;
    private String patientId;
    private String studyInstanceUid;
    private String accessionNumber;
    private String studyDate;
    private String description;
    private String patientBirthDate;
    private String patientSex;
    private String parsedFirstName;
    private String parsedLastName;

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

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

    public String getStudyDate() {
        return studyDate;
    }

    public void setStudyDate(String studyDate) {
        this.studyDate = studyDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPatientBirthDate() {
        return patientBirthDate;
    }

    public void setPatientBirthDate(String patientBirthDate) {
        this.patientBirthDate = patientBirthDate;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public String getParsedFirstName() {
        return parsedFirstName;
    }

    public void setParsedFirstName(String parsedFirstName) {
        this.parsedFirstName = parsedFirstName;
    }

    public String getParsedLastName() {
        return parsedLastName;
    }

    public void setParsedLastName(String parsedLastName) {
        this.parsedLastName = parsedLastName;
    }
}