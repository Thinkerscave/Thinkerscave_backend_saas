package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "class_subject_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_csm_class_subject",
                        columnNames = {"academic_class_id", "subject_id"}
                )
        },
        indexes = {
                @Index(name = "idx_csm_class_active", columnList = "academic_class_id, is_active"),
                @Index(name = "idx_csm_subject", columnList = "subject_id")
        }
)
public class ClassSubjectMapping extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_subject_mapping_id")
    @EqualsAndHashCode.Include
    private Long classSubjectMappingId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_class_id", nullable = false)
    private AcademicClass academicClass;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @NotNull
    @Positive
    @Column(name = "weekly_periods", nullable = false)
    private Short weeklyPeriods;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "timetable_preference", nullable = false, length = 30)
    private SubjectTimetablePreference timetablePreference = SubjectTimetablePreference.ANY;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
