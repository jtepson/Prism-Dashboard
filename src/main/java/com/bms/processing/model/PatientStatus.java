package com.bms.processing.model;

public enum PatientStatus {
    UPCOMING,
    VERIFYING,
    ACQUIRED,
    PROCESSING,
    PROCESSED,
    PROCESSED_WITH_ERRORS,
    PROCESSED_WITH_THIRD_PARTY_ERRORS,
    COMPLETED,
    ERROR
}
