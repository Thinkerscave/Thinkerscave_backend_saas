package com.thinkerscave.finance.payroll.dto.response;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SalaryStructureResponse {
    private Long salaryStructureId;
    private String name;
    private String description;
    private String applicableStaffType;
    private ComponentStatus status;
    private List<Item> items;

    @Data
    @Builder
    public static class Item {
        private Long salaryStructureItemId;
        private Long salaryComponentId;
        private String componentCode;
        private String componentName;
        private CalculationMethod calculationMethod;
        private BigDecimal value;
    }
}
