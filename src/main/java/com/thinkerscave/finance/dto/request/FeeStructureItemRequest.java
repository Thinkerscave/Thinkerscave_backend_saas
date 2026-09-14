package com.thinkerscave.finance.dto.request;

import com.thinkerscave.finance.enums.FeeFrequency;
import com.thinkerscave.finance.enums.FeeItemType;
import com.thinkerscave.finance.enums.FeeServiceKey;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeStructureItemRequest {
    @NotNull
    private Long feeHeadId;
    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
    @NotNull
    private FeeFrequency frequency;
    @NotNull
    private FeeItemType type;
    private FeeServiceKey serviceKey;
}
