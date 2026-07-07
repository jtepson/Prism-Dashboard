package com.bms.processing.model;

public enum PatientStatus {
    UPCOMING,
    VERIFYING,
    ACQUIRED,
    PROCESSING,
    PROCESSED,
    COMPLETED,

    ON_HOLD,
    MISSING_DATA,
    RESCAN_REQUIRED,
    REPORT_CORRECTION_REQUIRED,
    CANCELLED
}