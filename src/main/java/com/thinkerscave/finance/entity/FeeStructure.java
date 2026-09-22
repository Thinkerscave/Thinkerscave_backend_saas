package com.thinkerscave.finance.entity;

import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "fee_structure", indexes = {
        @Index(name = "idx_fee_structure_year_class", columnList = "academic_year_id, class_id"),
        @Index(name = "idx_fee_structure_status", columnList = "status")
})
public class FeeStructure extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_structure_id")
    @EqualsAndHashCode.Include
    private Long feeStructureId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "due_day", nullable = false)
    private Short dueDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeeMasterStatus status = FeeMasterStatus.ACTIVE;
}
