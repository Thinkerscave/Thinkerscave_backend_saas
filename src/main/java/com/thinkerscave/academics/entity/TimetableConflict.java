package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TimetableConflictStatus;
import com.thinkerscave.academics.enums.TimetableConflictType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "timetable_conflict",
        indexes = {
                @Index(name = "idx_timetable_conflict_version_status", columnList = "timetable_version_id, status"),
                @Index(name = "idx_timetable_conflict_blocking", columnList = "timetable_version_id, is_blocking"),
                @Index(name = "idx_timetable_conflict_allocation", columnList = "teacher_allocation_id")
        }
)
public class TimetableConflict extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_conflict_id")
    @EqualsAndHashCode.Include
    private Long timetableConflictId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_version_id", nullable = false)
    private TimetableVersion timetableVersion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_type", nullable = false, length = 50)
    private TimetableConflictType conflictType;

    @NotNull
    @Column(name = "is_blocking", nullable = false)
    private Boolean blocking = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TimetableConflictStatus status = TimetableConflictStatus.OPEN;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_entry_id")
    private TimetableEntry timetableEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_timetable_entry_id")
    private TimetableEntry relatedTimetableEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private AcademicSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_allocation_id")
    private TeacherAllocation teacherAllocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private AcademicResource resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 15)
    private DayOfWeek dayOfWeek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_period_id")
    private TimetablePeriod timetablePeriod;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;
}
