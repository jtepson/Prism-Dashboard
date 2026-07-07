package com.bms.processing.service;

import com.bms.processing.entity.CaseIssueEntity;
import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.model.CaseIssueStatus;
import com.bms.processing.repository.CaseIssueRepository;

import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<CaseIssueEntity> findByStatus(CaseIssueStatus status) {
        return caseIssueRepository.findByStatus(status);
    }
}