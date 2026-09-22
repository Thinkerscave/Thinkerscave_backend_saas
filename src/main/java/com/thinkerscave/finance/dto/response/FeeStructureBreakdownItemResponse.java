package com.thinkerscave.finance.dto.response;

import com.thinkerscave.finance.enums.FeeFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FeeStructureBreakdownItemResponse {
    private String feeHeadName;
    private FeeFrequency frequency;
    private BigDecimal amount;
}
