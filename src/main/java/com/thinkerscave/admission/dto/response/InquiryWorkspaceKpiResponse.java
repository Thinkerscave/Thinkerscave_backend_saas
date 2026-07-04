package com.thinkerscave.admission.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InquiryWorkspaceKpiResponse {

    private long newInquiries;
    private long todaysFollowUps;
    private long interested;
    private long admissionReady;
    private long futureProspects;
    private long closed;
}