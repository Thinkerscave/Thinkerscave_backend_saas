package com.thinkerscave.academics.enums;

/**
 * Operational allocation state. Soft deactivation uses {@code is_active}, not this enum.
 */
public enum TeacherAllocationStatus {
    UNASSIGNED,
    ASSIGNED,
    CONFLICT
}
