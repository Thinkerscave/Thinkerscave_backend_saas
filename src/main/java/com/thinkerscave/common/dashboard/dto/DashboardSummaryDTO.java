package com.thinkerscave.common.dashboard.dto;

import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregated KPI snapshot for an organization's dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDTO {
    private Long organizationId;
    private long totalStudents;
    private long activeStudents;
    private long totalStaff;
    private long activeStaff;
    private long activeEnrollments;
    private long openInquiries;
    private long pendingAdmissions;
    private long unpaidInvoices;
    private long overdueInvoices;
    @Builder.Default
    private Map<String, Long> extra = new LinkedHashMap<>();
}
