package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.TimetableSlotKind;
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
        name = "timetable_period",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timetable_period_config_number",
                        columnNames = {"timetable_configuration_id", "period_number"}
                )
        },
        indexes = {
                @Index(name = "idx_timetable_period_config_start", columnList = "timetable_configuration_id, start_time")
        }
)
public class TimetablePeriod extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_period_id")
    @EqualsAndHashCode.Include
    private Long timetablePeriodId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_configuration_id", nullable = false)
    private TimetableConfiguration timetableConfiguration;

    @NotNull
    @Positive
    @Column(name = "period_number", nullable = false)
    private Short periodNumber;

    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "slot_kind", nullable = false, length = 20)
    private TimetableSlotKind slotKind;
}
