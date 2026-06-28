package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.DiscountType;
import com.thinkerscave.platform.enums.PromotionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PromotionResponse {

    private Long id;
    private String promotionCode;
    private String promotionName;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maximumDiscount;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer maximumUsage;
    private Integer usedCount;
    private Boolean allowCustomPlan;
    private Boolean stackable;
    private Boolean autoApply;
    private PromotionStatus status;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
