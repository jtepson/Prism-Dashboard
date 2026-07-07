package com.bms.processing.model;


//Second part here, redesigning error allocation 70702026
public enum CaseIssueType {
    MISSING_DATA,
    INADEQUATE_SCAN_DATA,
    REPORT_CORRECTION,
    PRISM_PROCESSING,
    ACQUISITION,
    QA_ARTIFACT,
    ADMIN,
    OTHER
}