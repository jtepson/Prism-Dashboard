package com.bms.processing.model;

public enum AuditEventType {

    // Case timeline
    STATUS_CHANGED,
    FILE_UPLOADED,
    FILE_DOWNLOADED,
    REPORT_RETRIEVED,
    CASE_COMPLETED,
    CASE_CREATED,
    CASE_UPDATED,

    // Admin/security audit
    VIEW_CASE,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    DICOM_QUERY,
    DICOM_RETRIEVE,
    CONFIG_CHANGED,
    USER_CREATED,
    USER_UPDATED,
    PERMISSION_DENIED
}