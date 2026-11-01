package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.TimetableGenerationRunStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** One explicit generation/regeneration request and its result report. */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "timetable_generation_run",
        indexes = {
                @Index(name = "idx_timetable_generation_run_version",
                        columnList = "timetable_version_id")
        }
)
public class TimetableGenerationRun extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "generation_run_id")
    @EqualsAndHashCode.Include
    private Long generationRunId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_version_id", nullable = false)
    private TimetableVersion timetableVersion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TimetableGenerationRunStatus status = TimetableGenerationRunStatus.PENDING;

    @Column(name = "seed")
    private Long seed;

    @Column(name = "requested_group_ids", length = 2000)
    private String requestedGroupIds;

    @Lob
    @Column(name = "report")
    private String report;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;
}
