package com.thinkerscave.finance.payroll.dto.request;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.enums.StatutoryCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryComponentRequest {
    @NotBlank @Size(max = 40)
    private String code;
    @NotBlank @Size(max = 100)
    private String name;
    @NotNull
    private ComponentType componentType;
    @NotNull
    private CalculationMethod calculationMethod;
    private BigDecimal defaultValue;
    private StatutoryCode statutoryCode;
    private ComponentStatus status;
    private Integer sortOrder;
}
