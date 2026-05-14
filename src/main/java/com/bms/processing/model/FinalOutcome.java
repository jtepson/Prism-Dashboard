package com.bms.processing.model;

public enum FinalOutcome {
    COMPLETED,
    PROCESSED,
    PROCESSED_WITH_THIRD_PARTY_ERRORS,
    PROCESSED_WITH_ERRORS,
    UNCOMPLETED,
    RESCAN_NEEDED
}
