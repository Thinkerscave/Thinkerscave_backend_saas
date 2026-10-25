package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Reusable catalog subject: organization-scoped (the tenant schema is the boundary), not tied to an academic year.
 * Curriculum details (weekly periods, requirement, section coverage) live on {@link AcademicGroupSubjectMapping}.
 * Name and code are unique organization-wide, case-insensitive, enforced by the database.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "subject")
public class Subject extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_id")
    @EqualsAndHashCode.Include
    private Long subjectId;

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 30)
    private SubjectType subjectType = SubjectType.ACADEMIC_SUBJECT;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;


    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
