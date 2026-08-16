package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.TimetableConfigurationStatus;
import com.thinkerscave.academics.enums.TimetableShiftType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "timetable_configuration",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timetable_config_year_shift",
                        columnNames = {"academic_year_id", "shift_type"}
                )
        },
        indexes = {
                @Index(name = "idx_timetable_config_year_status", columnList = "academic_year_id, status"),
                @Index(name = "idx_timetable_config_active", columnList = "is_active")
        }
)
public class TimetableConfiguration extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_configuration_id")
    @EqualsAndHashCode.Include
    private Long timetableConfigurationId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false, length = 30)
    private TimetableShiftType shiftType;

    @NotNull
    @Column(name = "school_start_time", nullable = false)
    private LocalTime schoolStartTime;

    @NotNull
    @Column(name = "school_end_time", nullable = false)
    private LocalTime schoolEndTime;

    @Positive
    @Column(name = "default_period_duration_min")
    private Short defaultPeriodDurationMin;

    @NotNull
    @Positive
    @Column(name = "max_teacher_weekly_periods", nullable = false)
    private Short maxTeacherWeeklyPeriods;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TimetableConfigurationStatus status = TimetableConfigurationStatus.DRAFT;

    @NotNull
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
