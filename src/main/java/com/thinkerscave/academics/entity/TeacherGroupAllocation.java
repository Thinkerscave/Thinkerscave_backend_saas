package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.staff.entity.Staff;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** Teacher assigned to a curriculum mapping for every section that has no section-level override. */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "teacher_group_allocation")
public class TeacherGroupAllocation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "teacher_group_allocation_id")
    @EqualsAndHashCode.Include
    private Long teacherGroupAllocationId;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_group_subject_mapping_id", nullable = false, unique = true)
    private AcademicGroupSubjectMapping mapping;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
