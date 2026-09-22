package com.thinkerscave.finance.payroll.dto.response;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.enums.StatutoryCode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalaryComponentResponse {
    private Long salaryComponentId;
    private String code;
    private String name;
    private ComponentType componentType;
    private CalculationMethod calculationMethod;
    private BigDecimal defaultValue;
    private StatutoryCode statutoryCode;
    private ComponentStatus status;
    private Integer sortOrder;
}
