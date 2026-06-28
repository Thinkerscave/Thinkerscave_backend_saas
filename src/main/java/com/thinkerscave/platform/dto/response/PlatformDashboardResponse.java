package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PlatformDashboardResponse {

    private long totalCustomers;
    private long totalOrganizations;
    private long activeOrganizations;
    private long trialOrganizations;
    private long suspendedOrganizations;
    private long renewalDue30Days;
    private long provisioningInProgress;
    private long totalSubscriptionPlans;
    private long activePromotions;
}
