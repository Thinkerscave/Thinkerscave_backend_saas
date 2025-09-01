package com.thinkerscave.common.admission.domain;

/**
 * Enumeration representing the possible statuses of an admission application.
 * <p>
 * This enum defines the valid states for an application, such as PENDING, APPROVED, REJECTED, and UNDER_REVIEW,
 * to be used in the ApplicationAdmission entity and related DTOs.
 *
 * @author Bibekananda Pradhan
 * @since 2023-06-06
 */
public enum ApplicationStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    UNDER_REVIEW
}
