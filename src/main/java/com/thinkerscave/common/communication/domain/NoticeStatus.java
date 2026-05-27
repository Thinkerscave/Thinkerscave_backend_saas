package com.thinkerscave.common.communication.domain;

/** Lifecycle of a {@link Notice}. */
public enum NoticeStatus {
    DRAFT,
    SCHEDULED,
    PUBLISHED,
    EXPIRED,
    ARCHIVED,
    CANCELLED
}
