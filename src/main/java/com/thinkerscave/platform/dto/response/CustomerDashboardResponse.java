package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CustomerDashboardResponse {

    private long totalCustomers;
    private long activeCustomers;
    private long trialCustomers;
    private long suspendedCustomers;
    private long archivedCustomers;
    private long totalOrganizations;
    private BigDecimal annualRevenue;
    private long renewals30Days;
}
