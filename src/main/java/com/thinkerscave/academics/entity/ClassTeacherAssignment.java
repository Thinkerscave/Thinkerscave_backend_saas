package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.staff.entity.Staff;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "class_teacher_assignment",
        indexes = {
                @Index(name = "idx_cta_section_effective", columnList = "section_id, effective_from"),
                @Index(name = "idx_cta_staff_effective", columnList = "staff_id, effective_from"),
                @Index(name = "idx_cta_active", columnList = "is_active")
        }
)
public class ClassTeacherAssignment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_teacher_assignment_id")
    @EqualsAndHashCode.Include
    private Long classTeacherAssignmentId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private AcademicSection section;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
