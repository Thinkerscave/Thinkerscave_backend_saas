package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SubscriptionPlanRequest {

    @NotBlank
    @Size(max = 50)
    private String planCode;

    @NotBlank
    @Size(max = 150)
    private String planName;

    @Size(max = 2000)
    private String description;

    @DecimalMin("0.00")
    private BigDecimal monthlyPrice;

    @DecimalMin("0.00")
    private BigDecimal quarterlyPrice;

    @DecimalMin("0.00")
    private BigDecimal halfYearlyPrice;

    @DecimalMin("0.00")
    private BigDecimal yearlyPrice;

    @Min(0)
    private Integer studentLimit;

    @Min(0)
    private Integer staffLimit;

    @Min(0)
    private Integer branchLimit;

    @Min(0)
    private Integer storageLimitGb;

    private Long apiRequestLimit;

    @Min(0)
    private Integer trialDays;

    private Integer displayOrder;

    private Boolean recommended;

    private Boolean customPlan;

    private Boolean visible;

    @Size(max = 1000)
    private String remarks;
}
