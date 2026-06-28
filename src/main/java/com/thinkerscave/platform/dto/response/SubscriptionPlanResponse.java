package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class SubscriptionPlanResponse {

    private Long id;
    private String planCode;
    private String planName;
    private String description;
    private BigDecimal monthlyPrice;
    private BigDecimal quarterlyPrice;
    private BigDecimal halfYearlyPrice;
    private BigDecimal yearlyPrice;
    private Integer studentLimit;
    private Integer staffLimit;
    private Integer branchLimit;
    private Integer storageLimitGb;
    private Long apiRequestLimit;
    private Integer trialDays;
    private Integer displayOrder;
    private Boolean recommended;
    private Boolean customPlan;
    private Boolean visible;
    private Boolean active;
    private String remarks;
    private List<SubscriptionPlanFeatureResponse> features;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
