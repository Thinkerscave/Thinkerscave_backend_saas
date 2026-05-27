package com.thinkerscave.common.fee.domain;

/** Kind of adjustment applied to a {@link FeeInvoice}. */
public enum AdjustmentType {
    DISCOUNT,
    SCHOLARSHIP,
    CONCESSION,
    WAIVER,
    PENALTY,
    LATE_FEE,
    WRITE_OFF
}
