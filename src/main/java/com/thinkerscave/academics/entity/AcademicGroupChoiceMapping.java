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
        name = "academic_group_choice_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_group_choice_mapping",
                columnNames = {"academic_group_id", "subject_choice_group_id"}
        ),
        indexes = @Index(
                name = "idx_academic_group_choice_mapping_active",
                columnList = "academic_group_id, is_active"
        )
)
public class AcademicGroupChoiceMapping extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "academic_group_choice_mapping_id")
    @EqualsAndHashCode.Include
    private Long academicGroupChoiceMappingId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_group_id", nullable = false)
    private AcademicGroup academicGroup;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_choice_group_id", nullable = false)
    private SubjectChoiceGroup choiceGroup;

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
