package com.thinkerscave.finance.payroll.entity;

import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "salary_structure")
public class SalaryStructure extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_structure_id")
    @EqualsAndHashCode.Include
    private Long salaryStructureId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "applicable_staff_type", length = 30)
    private String applicableStaffType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ComponentStatus status = ComponentStatus.ACTIVE;
}
