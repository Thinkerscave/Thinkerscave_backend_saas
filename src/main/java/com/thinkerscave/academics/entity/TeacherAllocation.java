package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.TeacherAllocationStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "teacher_allocation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_teacher_allocation_section_mapping",
                        columnNames = {"section_id", "class_subject_mapping_id"}
                )
        },
        indexes = {
                @Index(name = "idx_teacher_allocation_section_status", columnList = "section_id, status"),
                @Index(name = "idx_teacher_allocation_mapping_status", columnList = "class_subject_mapping_id, status"),
                @Index(name = "idx_teacher_allocation_active", columnList = "is_active")
        }
)
public class TeacherAllocation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "teacher_allocation_id")
    @EqualsAndHashCode.Include
    private Long teacherAllocationId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private AcademicSection section;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_subject_mapping_id", nullable = false)
    private ClassSubjectMapping classSubjectMapping;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TeacherAllocationStatus status = TeacherAllocationStatus.UNASSIGNED;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
