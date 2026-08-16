package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.AcademicYearPattern;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academic_year",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_academic_year_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_academic_year_status", columnList = "status"),
                @Index(name = "idx_academic_year_dates", columnList = "start_date, end_date"),
                @Index(name = "idx_academic_year_active", columnList = "is_active")
        }
)
public class AcademicYear extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "academic_year_id")
    @EqualsAndHashCode.Include
    private Long academicYearId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "pattern", nullable = false, length = 30)
    private AcademicYearPattern pattern = AcademicYearPattern.ANNUAL;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AcademicYearStatus status = AcademicYearStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submitted_by_user_id")
    private Long submittedByUserId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId;

    @Size(max = 1000)
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "activated_by_user_id")
    private Long activatedByUserId;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    /** Cross-module compatibility alias during Academics rebuild. */
    public String getYearCode() {
        return name;
    }

    /** Cross-module compatibility alias during Academics rebuild. */
    public String getYearName() {
        return name;
    }

    /** Cross-module compatibility alias during Academics rebuild. */
    public boolean isCurrentYear() {
        return status == AcademicYearStatus.CURRENT;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
