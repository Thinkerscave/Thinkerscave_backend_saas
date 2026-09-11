package com.thinkerscave.admission.enums;

public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    /** Returned to counselor/applicant for corrections before resubmission. */
    ACTION_REQUIRED,
    DOCUMENTS_PENDING,
    FEE_PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    ENROLLED
}
