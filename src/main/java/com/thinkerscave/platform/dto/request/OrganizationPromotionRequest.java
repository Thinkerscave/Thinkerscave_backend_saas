package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationPromotionRequest {

    @NotNull
    private Long organizationId;

    @NotNull
    private Long promotionId;

    @Size(max = 1000)
    private String remarks;
}
