package com.thinkerscave.common.fee.domain;

/** Cadence at which a fee is charged. */
public enum FeeFrequency {
    ONE_TIME,
    MONTHLY,
    QUARTERLY,
    HALF_YEARLY,
    ANNUAL,
    TERM
}
