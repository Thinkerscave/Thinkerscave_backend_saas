package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class OrganizationPromotionResponse {

    private Long id;
    private Long organizationId;
    private String organizationName;
    private Long promotionId;
    private String promotionCode;
    private String promotionName;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String appliedBy;
    private Boolean applied;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
}
