package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.TimetableGenerationStatus;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "timetable_version",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timetable_version_year_number",
                        columnNames = {"academic_year_id", "version_number"}
                )
        },
        indexes = {
                @Index(name = "idx_timetable_version_year_status", columnList = "academic_year_id, status"),
                @Index(name = "idx_timetable_version_config", columnList = "timetable_configuration_id")
        }
)
public class TimetableVersion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_version_id")
    @EqualsAndHashCode.Include
    private Long timetableVersionId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_configuration_id", nullable = false)
    private TimetableConfiguration timetableConfiguration;

    @NotNull
    @Positive
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 40)
    private TimetableGenerationStatus generationStatus = TimetableGenerationStatus.NOT_GENERATED;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private TimetableStatus status = TimetableStatus.DRAFT;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by_user_id")
    private Long publishedByUserId;

    @Column(name = "superseded_at")
    private LocalDateTime supersededAt;
}
