package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillingPeriodOptionResponse {
    private String periodKey;
    private String periodLabel;
}
