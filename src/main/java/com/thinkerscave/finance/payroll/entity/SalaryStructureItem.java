package com.thinkerscave.finance.payroll.entity;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
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
@Table(name = "salary_structure_item", uniqueConstraints = {
        @UniqueConstraint(name = "uk_salary_structure_item", columnNames = {"salary_structure_id", "salary_component_id"})
})
public class SalaryStructureItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_structure_item_id")
    @EqualsAndHashCode.Include
    private Long salaryStructureItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salary_structure_id", nullable = false)
    private SalaryStructure salaryStructure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salary_component_id", nullable = false)
    private SalaryComponent salaryComponent;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, length = 30)
    private CalculationMethod calculationMethod;

    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;
}
