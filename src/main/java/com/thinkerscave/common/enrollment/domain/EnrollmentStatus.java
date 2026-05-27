package com.thinkerscave.common.enrollment.domain;

/**
 * Lifecycle states for an {@link AcademicEnrollment} row.
 */
public enum EnrollmentStatus {
    ACTIVE,
    PROMOTED,
    GRADUATED,
    TRANSFERRED_OUT,
    DROPPED_OUT,
    EXPELLED,
    DECEASED,
    ON_HOLD
}
