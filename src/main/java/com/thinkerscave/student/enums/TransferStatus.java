package com.thinkerscave.student.enums;

/** Lifecycle of a {@link TransferRequest}. */
public enum TransferStatus {
    REQUESTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    CERTIFICATE_ISSUED,
    CANCELLED
}
