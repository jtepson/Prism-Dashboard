package com.bms.processing.entity;

import com.bms.processing.model.CaseIssueStatus;
import com.bms.processing.model.CaseIssueType;
import com.bms.processing.model.CaseIssueSource;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_issue")
public class CaseIssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_record_id", nullable = false)
    private CaseRecordEntity caseRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_source", nullable = false)
    private CaseIssueSource issueSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private CaseIssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseIssueStatus status = CaseIssueStatus.ACTIVE;

    @Column(nullable = false)
    private Boolean blocking = true;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String resolutionNote;

    public CaseIssueEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CaseRecordEntity getCaseRecord() {
        return caseRecord;
    }

    public void setCaseRecord(CaseRecordEntity caseRecord) {
        this.caseRecord = caseRecord;
    }

    public CaseIssueSource getIssueSource() {
        return issueSource;
    }

    public void setIssueSource(CaseIssueSource issueSource) {
        this.issueSource = issueSource;
    }

    public CaseIssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(CaseIssueType issueType) {
        this.issueType = issueType;
    }

    public CaseIssueStatus getStatus() {
        return status;
    }

    public void setStatus(CaseIssueStatus status) {
        this.status = status;
    }

    public Boolean getBlocking() {
        return blocking;
    }

    public void setBlocking(Boolean blocking) {
        this.blocking = blocking;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }
}