package com.thinkerscave.finance.dto.response;

import com.thinkerscave.finance.enums.FeeMasterStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class FeeStructureResponse {
    private Long feeStructureId;
    private String name;
    private Long academicYearId;
    private Long classId;
    private Short dueDay;
    private FeeMasterStatus status;
    private BigDecimal mandatorySum;
    private BigDecimal optionalSum;
    private BigDecimal configuredMonthlyAmount;
    private List<FeeStructureItemResponse> items;
}
