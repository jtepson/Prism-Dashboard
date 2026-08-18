package com.bms.processing.service;

import com.bms.processing.entity.CaseIssueEntity;
import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.CaseIssueStatus;
import com.bms.processing.repository.CaseIssueRepository;
import com.bms.processing.model.CaseIssueType;
import com.bms.processing.model.CaseIssueSource;

import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class CaseIssueService {

    private final CaseIssueRepository caseIssueRepository;
    private final AuditEventService auditEventService;

    public CaseIssueService(
        CaseIssueRepository caseIssueRepository,
        AuditEventService auditEventService
    ) {
        this.caseIssueRepository = caseIssueRepository;
        this.auditEventService = auditEventService;
    }

    public List<CaseIssueEntity> findAll() {
        return caseIssueRepository.findAll();
    }

    public CaseIssueEntity save(CaseIssueEntity issue) {
        return caseIssueRepository.save(issue);
    }

    public void delete(CaseIssueEntity issue) {
        caseIssueRepository.delete(issue);
    }

    public List<CaseIssueEntity> findByCaseRecord(CaseRecordEntity caseRecord) {
        return caseIssueRepository.findByCaseRecord(caseRecord);
    }

    public List<CaseIssueEntity> findActiveByCaseRecord(CaseRecordEntity caseRecord) {
        return caseIssueRepository.findByCaseRecordAndStatus(
                caseRecord,
                CaseIssueStatus.ACTIVE
        );
    }

    public CaseIssueEntity createIssue(
            CaseRecordEntity caseRecord,
            CaseIssueSource issueSource,
            CaseIssueType issueType,
            boolean blocking,
            String title,
            String description,
            String createdBy
    ) {
        CaseIssueEntity issue = new CaseIssueEntity();

        issue.setCaseRecord(caseRecord);
        issue.setIssueType(issueType);
        issue.setStatus(CaseIssueStatus.ACTIVE);
        issue.setBlocking(blocking);
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setCreatedBy(createdBy);
        issue.setCreatedAt(LocalDateTime.now());
        issue.setIssueSource(issueSource);

        //added in to account for issue resolution
        CaseIssueEntity savedIssue = caseIssueRepository.save(issue);
        auditEventService.logTimelineEvent(
                "CASE_ISSUE_CREATED",
                caseRecord,
                savedIssue.getTitle(),
                null,
                savedIssue.getDescription(),
                createdBy
        );

        return savedIssue;
    }

    //added this for issue updated, for better tracking 08182026
    public CaseIssueEntity updateIssue(
            CaseIssueEntity issue,
            CaseIssueSource issueSource,
            CaseIssueType issueType,
            boolean blocking,
            String title,
            String description,
            String updatedBy
    ) {
        String oldValue =
                "Source=" + issue.getIssueSource()
                        + ", Category=" + issue.getIssueType()
                        + ", Blocking=" + issue.getBlocking()
                        + ", Note=" + issue.getDescription();

        issue.setIssueSource(issueSource);
        issue.setIssueType(issueType);
        issue.setBlocking(blocking);
        issue.setTitle(title);
        issue.setDescription(description);

        CaseIssueEntity savedIssue = caseIssueRepository.save(issue);

        String newValue =
                "Source=" + savedIssue.getIssueSource()
                        + ", Category=" + savedIssue.getIssueType()
                        + ", Blocking=" + savedIssue.getBlocking()
                        + ", Note=" + savedIssue.getDescription();

        auditEventService.logTimelineEvent(
                "CASE_ISSUE_UPDATED",
                savedIssue.getCaseRecord(),
                savedIssue.getTitle(),
                oldValue,
                newValue,
                updatedBy
        );

        return savedIssue;
    }

    public CaseIssueEntity resolveIssue(
        CaseIssueEntity issue,
        String resolvedBy,
        String resolutionNote
    ) {
        issue.setStatus(CaseIssueStatus.RESOLVED);
        issue.setResolvedBy(resolvedBy);
        issue.setResolvedAt(LocalDateTime.now());
        issue.setResolutionNote(resolutionNote);

        CaseIssueEntity savedIssue = caseIssueRepository.save(issue);
        auditEventService.logTimelineEvent(
                "CASE_ISSUE_RESOLVED",
                savedIssue.getCaseRecord(),
                savedIssue.getTitle(),
                "ACTIVE",
                resolutionNote,
                resolvedBy
        );

        return savedIssue;
    }

    public List<CaseIssueEntity> findByStatus(CaseIssueStatus status) {
        return caseIssueRepository.findByStatus(status);
    }

    public List<CaseIssueEntity> findByStatusWithCaseRecord(CaseIssueStatus status) {
        return caseIssueRepository.findByStatusWithCaseRecord(status);
    }
    
}