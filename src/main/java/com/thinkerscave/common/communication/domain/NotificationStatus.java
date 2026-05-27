package com.thinkerscave.common.communication.domain;

/** Delivery state of a notification dispatch attempt. */
public enum NotificationStatus {
    PENDING,
    QUEUED,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    CANCELLED
}
