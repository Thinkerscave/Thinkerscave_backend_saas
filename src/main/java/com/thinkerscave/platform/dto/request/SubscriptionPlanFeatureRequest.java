package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionPlanFeatureRequest {

    @NotNull
    private Long subscriptionPlanId;

    @NotNull
    private Long featureId;

    private Boolean enabled;

    private Boolean mandatory;

    private Integer displayOrder;

    @Size(max = 1000)
    private String notes;
}
