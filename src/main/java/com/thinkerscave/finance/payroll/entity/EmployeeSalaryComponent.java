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
@Table(name = "employee_salary_component", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_salary_component",
                columnNames = {"employee_salary_id", "salary_component_id"})
})
public class EmployeeSalaryComponent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_salary_component_id")
    @EqualsAndHashCode.Include
    private Long employeeSalaryComponentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_salary_id", nullable = false)
    private EmployeeSalary employeeSalary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salary_component_id", nullable = false)
    private SalaryComponent salaryComponent;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 30)
    private ComponentType componentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, length = 30)
    private CalculationMethod calculationMethod;

    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "applicable", nullable = false)
    private Boolean applicable = true;
}
