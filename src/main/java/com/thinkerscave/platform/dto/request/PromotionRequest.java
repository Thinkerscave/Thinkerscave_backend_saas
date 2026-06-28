package com.thinkerscave.platform.dto.request;

import com.thinkerscave.platform.enums.DiscountType;
import com.thinkerscave.platform.enums.PromotionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PromotionRequest {

    @NotBlank
    @Size(max = 50)
    private String promotionCode;

    @NotBlank
    @Size(max = 150)
    private String promotionName;

    @Size(max = 2000)
    private String description;

    @NotNull
    private DiscountType discountType;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal discountValue;

    @DecimalMin("0.00")
    private BigDecimal maximumDiscount;

    private LocalDate validFrom;

    private LocalDate validTo;

    @Min(0)
    private Integer maximumUsage;

    private Boolean allowCustomPlan;

    private Boolean stackable;

    private Boolean autoApply;

    private PromotionStatus status;

    @Size(max = 1000)
    private String remarks;
}
