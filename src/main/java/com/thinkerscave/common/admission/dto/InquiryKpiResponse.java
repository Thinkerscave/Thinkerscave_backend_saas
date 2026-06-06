package com.thinkerscave.common.admission.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * KPI summary for the Inquiry Center landing workspace.
 * Mirrors the six spec-required smart KPI cards.
 */
@Getter
@Setter
@Builder
public class InquiryKpiResponse {
    private long newInquiries;
    private long todaysFollowUps;
    private long interested;
    private long admissionReady;
    private long futureProspects;
    private long closed;
}
