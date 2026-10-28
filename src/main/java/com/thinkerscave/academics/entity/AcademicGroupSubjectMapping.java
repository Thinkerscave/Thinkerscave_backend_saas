package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.enums.SubjectRequirement;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academic_group_subject_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_group_subject_mapping_subject",
                        columnNames = {"academic_group_id", "subject_id"}
                )
        },
        indexes = {
                @Index(name = "idx_group_subject_mapping_active", columnList = "academic_group_id, is_active"),
                @Index(name = "idx_group_subject_mapping_subject", columnList = "subject_id")
        }
)
public class AcademicGroupSubjectMapping extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "academic_group_subject_mapping_id")
    @EqualsAndHashCode.Include
    private Long academicGroupSubjectMappingId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_group_id", nullable = false)
    private AcademicGroup academicGroup;

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
    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 20)
    private SubjectRequirement requirement = SubjectRequirement.COMPULSORY;

    @Size(max = 60)
    @Column(name = "choice_group", length = 60)
    private String choiceGroup;

    @NotNull
    @Column(name = "consecutive_periods", nullable = false)
    private Short consecutivePeriods = 1;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
