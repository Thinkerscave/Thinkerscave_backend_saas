package com.thinkerscave.finance.dto.request;

import com.thinkerscave.finance.enums.FeeMasterStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentMethodRequest {
    @NotBlank @Size(max = 80)
    private String name;
    @Size(max = 255)
    private String description;
    private FeeMasterStatus status;
    private Boolean requiresReference;
    private Integer sortOrder;
}
