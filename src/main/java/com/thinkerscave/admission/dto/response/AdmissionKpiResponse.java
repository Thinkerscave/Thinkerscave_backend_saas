package com.thinkerscave.admission.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Schema(description = "Admission module dashboard KPI summary")
public class AdmissionKpiResponse {

    private long totalInquiries;
    private long newInquiries;
    private long activeInquiries;
    private long convertedInquiries;
    private long lostInquiries;
    private long pendingFollowUps;

    private long totalApplications;
    private long draftApplications;
    private long pendingApplications;
    private long approvedApplications;
    private long rejectedApplications;

    private Map<String, Long> inquiryStatusBreakdown;
}
