package com.thinkerscave.finance.payroll.entity;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.enums.StatutoryCode;
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
@Table(name = "salary_component", indexes = {
        @Index(name = "idx_salary_component_status", columnList = "status")
})
public class SalaryComponent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_component_id")
    @EqualsAndHashCode.Include
    private Long salaryComponentId;

    @Column(name = "code", nullable = false, length = 40, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 30)
    private ComponentType componentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, length = 30)
    private CalculationMethod calculationMethod;

    @Column(name = "default_value", precision = 12, scale = 2)
    private BigDecimal defaultValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "statutory_code", length = 20)
    private StatutoryCode statutoryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ComponentStatus status = ComponentStatus.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
