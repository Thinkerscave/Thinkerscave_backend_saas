package com.thinkerscave.audit.enums;

/** Categorises entries in {@code audit_log}. */
public enum AuditEventType {
    CREATE,
    UPDATE,
    DELETE,
    STATE_CHANGE,
    APPROVAL,
    REJECTION,
    LOGIN,
    LOGOUT,
    EXPORT,
    IMPORT,
    BULK_OPERATION,
    CONFIG_CHANGE,
    SYSTEM_EVENT
}
