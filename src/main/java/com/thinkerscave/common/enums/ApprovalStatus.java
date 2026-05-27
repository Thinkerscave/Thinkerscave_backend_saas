package com.thinkerscave.common.enums;

/**
 * Generic approval workflow state shared by leave requests, transfer
 * requests, refund requests, etc.
 */
public enum ApprovalStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
