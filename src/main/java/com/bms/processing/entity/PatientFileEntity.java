package com.bms.processing.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_file")
public class PatientFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_record_id")
    private Long caseRecordId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "source")
    private String source;

    @Column(name = "file_date")
    private LocalDate fileDate;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();

        if (fileDate == null) {
            fileDate = LocalDate.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCaseRecordId() {
        return caseRecordId;
    }

    public void setCaseRecordId(Long caseRecordId) {
        this.caseRecordId = caseRecordId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDate getFileDate() {
        return fileDate;
    }

    public void setFileDate(LocalDate fileDate) {
        this.fileDate = fileDate;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}