package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "timetable_working_day",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timetable_working_day_config_day",
                        columnNames = {"timetable_configuration_id", "day_of_week"}
                )
        }
)
public class TimetableWorkingDay extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_working_day_id")
    @EqualsAndHashCode.Include
    private Long timetableWorkingDayId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_configuration_id", nullable = false)
    private TimetableConfiguration timetableConfiguration;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 15)
    private DayOfWeek dayOfWeek;

    @NotNull
    @Column(name = "is_working", nullable = false)
    private Boolean working = true;
}
