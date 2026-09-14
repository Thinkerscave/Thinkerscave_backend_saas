package com.thinkerscave.finance.payroll.entity;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "employee_payroll_line", indexes = {
        @Index(name = "idx_epl_payroll", columnList = "employee_payroll_id")
})
public class EmployeePayrollLine extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_payroll_line_id")
    @EqualsAndHashCode.Include
    private Long employeePayrollLineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_payroll_id", nullable = false)
    private EmployeePayroll employeePayroll;

    @Column(name = "salary_component_id")
    private Long salaryComponentId;

    @Column(name = "component_code", nullable = false, length = 40)
    private String componentCode;

    @Column(name = "component_name", nullable = false, length = 100)
    private String componentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 30)
    private ComponentType componentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, length = 30)
    private CalculationMethod calculationMethod;

    @Column(name = "rate_or_percent", precision = 12, scale = 2)
    private BigDecimal rateOrPercent;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
