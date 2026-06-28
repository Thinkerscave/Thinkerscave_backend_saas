package com.thinkerscave.platform.dto.request;

import com.thinkerscave.platform.enums.BillingCycle;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OrganizationSubscriptionRequest {

    @NotNull
    private Long organizationId;

    @NotNull
    private Long subscriptionPlanId;

    private Long promotionId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private LocalDate trialEndDate;

    @NotNull
    private BillingCycle billingCycle;

    private Integer studentLimitOverride;

    private Integer staffLimitOverride;

    private Integer branchLimitOverride;

    private Integer storageLimitOverride;

    private Boolean autoRenew;

    @Size(max = 1000)
    private String remarks;
}
