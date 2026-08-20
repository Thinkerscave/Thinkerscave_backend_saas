package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.BillingCycle;
import com.thinkerscave.platform.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class OrganizationSubscriptionResponse {

    private Long id;
    private Long organizationId;
    private String organizationName;
    private String organizationCode;
    private Long subscriptionPlanId;
    private String planCode;
    private String planName;
    private Long promotionId;
    private String promotionCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate trialEndDate;
    private BillingCycle billingCycle;
    private BigDecimal planPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private Integer studentLimitOverride;
    private Integer staffLimitOverride;
    private Integer branchLimitOverride;
    private Integer storageLimitOverride;
    private Integer studentLimit;
    private Integer staffLimit;
    private Integer branchLimit;
    private Integer storageLimitGb;
    private String invoiceNumber;
    private Boolean autoRenew;
    private SubscriptionStatus status;
    private Boolean active;
    private String remarks;
    private List<FeatureOverrideResponse> featureOverrides;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
