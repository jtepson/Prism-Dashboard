package com.bms.processing.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "case_secure_share")
public class CaseSecureShareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_record_id", nullable = false)
    private CaseRecordEntity caseRecord;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "access_code_hash")
    private String accessCodeHash;

    @Column(name = "allow_view", nullable = false)
    private Boolean allowView = true;

    @Column(name = "allow_download", nullable = false)
    private Boolean allowDownload = true;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "max_downloads")
    private Integer maxDownloads;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @ManyToMany
    @JoinTable(
            name = "case_secure_share_file",
            joinColumns = @JoinColumn(name = "secure_share_id"),
            inverseJoinColumns = @JoinColumn(name = "patient_file_id")
    )
    private Set<PatientFileEntity> files = new HashSet<>();

    public Long getId() {
        return id;
    }

    public CaseRecordEntity getCaseRecord() {
        return caseRecord;
    }

    public void setCaseRecord(CaseRecordEntity caseRecord) {
        this.caseRecord = caseRecord;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getAccessCodeHash() {
        return accessCodeHash;
    }

    public void setAccessCodeHash(String accessCodeHash) {
        this.accessCodeHash = accessCodeHash;
    }

    public Boolean getAllowView() {
        return allowView;
    }

    public void setAllowView(Boolean allowView) {
        this.allowView = allowView;
    }

    public Boolean getAllowDownload() {
        return allowDownload;
    }

    public void setAllowDownload(Boolean allowDownload) {
        this.allowDownload = allowDownload;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Set<PatientFileEntity> getFiles() {
        return files;
    }

    public void setFiles(Set<PatientFileEntity> files) {
        this.files = files;
    }
}