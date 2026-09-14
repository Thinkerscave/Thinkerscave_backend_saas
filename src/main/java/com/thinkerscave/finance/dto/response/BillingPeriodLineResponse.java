package com.thinkerscave.finance.dto.response;

import com.thinkerscave.finance.enums.FeeFrequency;
import com.thinkerscave.finance.enums.FeeItemType;
import com.thinkerscave.finance.enums.FeeServiceKey;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BillingPeriodLineResponse {
    private Long feeHeadId;
    private String feeHeadName;
    private String feeHeadCategory;
    private BigDecimal amount;
    private FeeFrequency frequency;
    private FeeItemType type;
    private FeeServiceKey serviceKey;
}
