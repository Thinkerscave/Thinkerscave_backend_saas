package com.thinkerscave.finance.payroll.dto.request;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class SalaryStructureRequest {
    @NotBlank @Size(max = 150)
    private String name;
    @Size(max = 500)
    private String description;
    @Size(max = 30)
    private String applicableStaffType;
    private ComponentStatus status;
    @Valid
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        @NotNull
        private Long salaryComponentId;
        @NotNull
        private CalculationMethod calculationMethod;
        @NotNull
        private BigDecimal value;
    }
}
