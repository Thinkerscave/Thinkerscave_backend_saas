package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FeatureOverrideResponse {

    private Long id;
    private Long organizationSubscriptionId;
    private Long featureId;
    private String featureCode;
    private String featureName;
    private String featureKey;
    private Boolean enabled;
    private String overrideReason;
    private LocalDate expiryDate;
    private Boolean complimentary;
    private Boolean chargeable;
    private BigDecimal additionalCharge;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
}
