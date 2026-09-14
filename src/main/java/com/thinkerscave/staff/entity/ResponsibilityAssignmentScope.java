package com.thinkerscave.staff.entity;

import com.thinkerscave.staff.enums.DataScopeType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Structured responsibility data scope (Access/Staff owned). Finance reads only.
 * Do not parse free-text ResponsibilityAssignment.scope.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "responsibility_assignment_scope", indexes = {
        @Index(name = "idx_ras_assignment", columnList = "assignment_id")
})
public class ResponsibilityAssignmentScope extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scope_id")
    @EqualsAndHashCode.Include
    private Long scopeId;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private DataScopeType scopeType;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "student_id")
    private Long studentId;
}
