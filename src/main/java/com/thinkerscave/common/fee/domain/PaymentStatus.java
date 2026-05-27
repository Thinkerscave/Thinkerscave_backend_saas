package com.thinkerscave.common.fee.domain;

/** Lifecycle of a {@link FeePayment}. */
public enum PaymentStatus {
    INITIATED,
    PENDING,
    SUCCESS,
    FAILED,
    REVERSED,
    REFUNDED
}
