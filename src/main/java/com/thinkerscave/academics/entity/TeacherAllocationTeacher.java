package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
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
        name = "teacher_allocation_teacher",
        indexes = {
                @Index(name = "idx_tat_staff_effective", columnList = "staff_id, effective_from"),
                @Index(name = "idx_tat_allocation_effective", columnList = "teacher_allocation_id, effective_from"),
                @Index(name = "idx_tat_active", columnList = "is_active")
        }
)
public class TeacherAllocationTeacher extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "teacher_allocation_teacher_id")
    @EqualsAndHashCode.Include
    private Long teacherAllocationTeacherId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_allocation_id", nullable = false)
    private TeacherAllocation teacherAllocation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private TeacherAllocationTeacherRole role = TeacherAllocationTeacherRole.SECONDARY;

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
