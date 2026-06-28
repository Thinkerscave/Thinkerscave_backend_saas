package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SubscriptionPlanFeatureResponse {

    private Long id;
    private Long subscriptionPlanId;
    private String planName;
    private Long featureId;
    private String featureCode;
    private String featureName;
    private String featureKey;
    private String module;
    private Boolean enabled;
    private Boolean mandatory;
    private Integer displayOrder;
    private String notes;
    private Boolean active;
    private LocalDateTime createdOn;
}
