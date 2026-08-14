package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.SubjectCategory;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
        name = "subject",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subject_year_code",
                        columnNames = {"academic_year_id", "code"}
                )
        },
        indexes = {
                @Index(name = "idx_subject_year_active", columnList = "academic_year_id, is_active")
        }
)
public class Subject extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_id")
    @EqualsAndHashCode.Include
    private Long subjectId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

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
    @Column(name = "category", nullable = false, length = 30)
    private SubjectCategory category;

    @NotNull
    @Positive
    @Column(name = "default_weekly_periods", nullable = false)
    private Short defaultWeeklyPeriods;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "timetable_preference", nullable = false, length = 30)
    private SubjectTimetablePreference timetablePreference = SubjectTimetablePreference.ANY;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    /** Cross-module compatibility alias during Academics rebuild. */
    public String getSubjectName() {
        return name;
    }

    /** Cross-module compatibility alias during Academics rebuild. */
    public String getSubjectCode() {
        return code;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
