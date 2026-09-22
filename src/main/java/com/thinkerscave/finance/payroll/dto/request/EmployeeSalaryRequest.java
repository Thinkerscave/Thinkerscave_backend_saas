package com.thinkerscave.finance.payroll.dto.request;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class EmployeeSalaryRequest {
    @NotNull
    private PaymentType paymentType;
    private Long salaryStructureId;
    @NotNull
    private LocalDate effectiveFrom;
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String remarks;
    @Valid
    private List<ComponentLine> components = new ArrayList<>();

    @Data
    public static class ComponentLine {
        @NotNull
        private Long salaryComponentId;
        private ComponentType componentType;
        @NotNull
        private CalculationMethod calculationMethod;
        @NotNull
        private BigDecimal value;
        private Boolean applicable = true;
    }
}
