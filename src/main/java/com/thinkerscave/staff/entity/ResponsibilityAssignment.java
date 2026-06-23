package com.thinkerscave.staff.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "responsibility_assignment",
        indexes = {
                @Index(name = "idx_assignment_staff", columnList = "staff_id"),
                @Index(name = "idx_assignment_responsibility", columnList = "responsibility_id"),
                @Index(name = "idx_assignment_active", columnList = "active")
        }
)
public class ResponsibilityAssignment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    @EqualsAndHashCode.Include
    private Long assignmentId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsibility_id", nullable = false)
    private Responsibility responsibility;

    /**
     * Example:
     * Class 8A
     * School Wide
     * Academic Session 2026
     */
    @Size(max = 200)
    @Column(name = "scope", length = 200)
    private String scope;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}