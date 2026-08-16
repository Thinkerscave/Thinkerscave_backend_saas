package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.TimetableShiftType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class TimetableConfigurationRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    private TimetableShiftType shiftType;

    @NotNull
    private LocalTime schoolStartTime;

    @NotNull
    private LocalTime schoolEndTime;

    @Positive
    private Short defaultPeriodDurationMin;

    @NotNull
    @Positive
    private Short maxTeacherWeeklyPeriods;

    @Valid
    private List<TimetableWorkingDayRequest> workingDays;

    @Valid
    private List<TimetablePeriodRequest> periods;
}
