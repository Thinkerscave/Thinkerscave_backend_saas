package com.thinkerscave.common.promotion.domain;

/** Lifecycle of a {@link TransferRequest}. */
public enum TransferStatus {
    REQUESTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    CERTIFICATE_ISSUED,
    CANCELLED
}
