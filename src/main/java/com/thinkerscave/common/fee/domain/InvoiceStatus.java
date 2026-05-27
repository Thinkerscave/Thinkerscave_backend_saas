package com.thinkerscave.common.fee.domain;

/** Lifecycle of an {@link FeeInvoice}. */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED,
    WRITTEN_OFF
}
