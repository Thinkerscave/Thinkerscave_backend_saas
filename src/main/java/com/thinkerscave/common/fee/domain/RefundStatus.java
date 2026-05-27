package com.thinkerscave.common.fee.domain;

/** Status of a {@link FeeRefund}. */
public enum RefundStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    PROCESSED,
    CANCELLED
}
