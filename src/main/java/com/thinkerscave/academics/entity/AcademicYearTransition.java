package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.AcademicTransitionStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
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
        name = "academic_year_transition",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_academic_year_transition_pair",
                        columnNames = {"source_academic_year_id", "target_academic_year_id"}
                )
        },
        indexes = {
                @Index(name = "idx_academic_year_transition_target_status", columnList = "target_academic_year_id, status")
        }
)
public class AcademicYearTransition extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "academic_year_transition_id")
    @EqualsAndHashCode.Include
    private Long academicYearTransitionId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_academic_year_id", nullable = false)
    private AcademicYear sourceAcademicYear;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_academic_year_id", nullable = false)
    private AcademicYear targetAcademicYear;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AcademicTransitionStatus status = AcademicTransitionStatus.NOT_STARTED;

    @NotNull
    @Column(name = "copy_classes", nullable = false)
    private Boolean copyClasses = false;

    @NotNull
    @Column(name = "copy_sections", nullable = false)
    private Boolean copySections = false;

    @NotNull
    @Column(name = "copy_subjects", nullable = false)
    private Boolean copySubjects = false;

    @NotNull
    @Column(name = "copy_mappings", nullable = false)
    private Boolean copyMappings = false;

    @NotNull
    @Column(name = "copy_allocations", nullable = false)
    private Boolean copyAllocations = false;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "activated_by_user_id")
    private Long activatedByUserId;

    @Size(max = 1000)
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;
}
