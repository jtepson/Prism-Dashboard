package com.bms.processing.service;

import com.bms.processing.entity.CaseIssueEntity;
import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.CaseIssueStatus;
import com.bms.processing.repository.CaseIssueRepository;
import com.bms.processing.model.CaseIssueType;

import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class CaseIssueService {

    private final CaseIssueRepository caseIssueRepository;

    public CaseIssueService(CaseIssueRepository caseIssueRepository) {
        this.caseIssueRepository = caseIssueRepository;
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

        return caseIssueRepository.save(issue);
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

        return caseIssueRepository.save(issue);
    }

    public List<CaseIssueEntity> findByStatus(CaseIssueStatus status) {
        return caseIssueRepository.findByStatus(status);
    }

    public List<CaseIssueEntity> findByStatusWithCaseRecord(CaseIssueStatus status) {
        return caseIssueRepository.findByStatusWithCaseRecord(status);
    }
    
}