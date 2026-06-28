package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class FeatureOverrideRequest {

    @NotNull
    private Long organizationSubscriptionId;

    @NotNull
    private Long featureId;

    private Boolean enabled;

    @Size(max = 500)
    private String overrideReason;

    private LocalDate expiryDate;

    private Boolean complimentary;

    private Boolean chargeable;

    @DecimalMin("0.00")
    private BigDecimal additionalCharge;

    @Size(max = 1000)
    private String remarks;
}
