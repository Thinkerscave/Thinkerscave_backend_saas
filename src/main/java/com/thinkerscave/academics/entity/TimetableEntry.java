package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TimetableEntryType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "timetable_entry",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timetable_entry_slot",
                        columnNames = {
                                "timetable_version_id",
                                "section_id",
                                "day_of_week",
                                "timetable_period_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_timetable_entry_version_day_period",
                        columnList = "timetable_version_id, day_of_week, timetable_period_id"
                ),
                @Index(
                        name = "idx_timetable_entry_allocation",
                        columnList = "teacher_allocation_id"
                ),
                @Index(
                        name = "idx_timetable_entry_resource",
                        columnList = "resource_id"
                )
        }
)
public class TimetableEntry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_entry_id")
    @EqualsAndHashCode.Include
    private Long timetableEntryId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_version_id", nullable = false)
    private TimetableVersion timetableVersion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 15)
    private DayOfWeek dayOfWeek;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_period_id", nullable = false)
    private TimetablePeriod timetablePeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_group_id")
    private AcademicGroup academicGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private AcademicSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_allocation_id")
    private TeacherAllocation teacherAllocation;

    /** Actual teacher placed on this lesson; may differ from the allocation's primary teacher. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private com.thinkerscave.staff.entity.Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_activity_id")
    private FixedActivity fixedActivity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private AcademicResource resource;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private TimetableEntryType entryType;

    @Size(max = 150)
    @Column(name = "subject_name_snapshot", length = 150)
    private String subjectNameSnapshot;

    @NotNull
    @Column(name = "is_locked", nullable = false)
    private Boolean locked = false;
}
