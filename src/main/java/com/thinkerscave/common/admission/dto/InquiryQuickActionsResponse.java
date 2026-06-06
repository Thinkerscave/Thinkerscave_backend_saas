package com.thinkerscave.common.admission.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Quick action counts displayed on the Inquiry Center workspace.
 * Helps counselors surface operational work for today.
 */
@Getter
@Setter
@Builder
public class InquiryQuickActionsResponse {
    private long todaysCalls;
    private long todaysMeetings;
    private long overdueFollowUps;
    private long admissionReady;
}
