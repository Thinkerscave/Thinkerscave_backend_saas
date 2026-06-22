package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academic_schedule",
        indexes = {
                @Index(name = "idx_schedule_start", columnList = "start_date"),
                @Index(name = "idx_schedule_end", columnList = "end_date")
        }
)
public class AcademicSchedule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    @EqualsAndHashCode.Include
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotBlank
    @Size(max = 100)
    @Column(name = "schedule_name", nullable = false)
    private String scheduleName;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}