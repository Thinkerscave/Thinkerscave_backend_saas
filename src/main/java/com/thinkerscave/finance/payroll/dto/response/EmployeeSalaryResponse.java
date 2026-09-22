package com.thinkerscave.finance.payroll.dto.response;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class EmployeeSalaryResponse {
    private Long employeeSalaryId;
    private Long staffId;
    private PaymentType paymentType;
    private Long salaryStructureId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String remarks;
    private List<ComponentLine> components;

    @Data
    @Builder
    public static class ComponentLine {
        private Long employeeSalaryComponentId;
        private Long salaryComponentId;
        private String componentCode;
        private String componentName;
        private ComponentType componentType;
        private CalculationMethod calculationMethod;
        private BigDecimal value;
        private Boolean applicable;
    }
}
