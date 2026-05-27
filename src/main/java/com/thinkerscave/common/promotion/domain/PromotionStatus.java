package com.thinkerscave.common.promotion.domain;

/** Lifecycle of a {@link PromotionBatch}. */
public enum PromotionStatus {
    DRAFT,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    ROLLED_BACK
}
