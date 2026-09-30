package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.TimetableConfigurationStatus;
import com.thinkerscave.academics.enums.TimetableShiftType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
public class TimetableConfigurationResponse {

    private Long timetableConfigurationId;
    private Long academicYearId;
    private String name;
    private TimetableShiftType shiftType;
    private LocalTime schoolStartTime;
    private LocalTime schoolEndTime;
    private Short defaultPeriodDurationMin;
    private Short maxTeacherWeeklyPeriods;
    private TimetableConfigurationStatus status;
    private Boolean isLocked;
    private Boolean active;
    private List<TimetableWorkingDayResponse> workingDays;
    private List<TimetablePeriodResponse> periods;
    /** Set on unsaved templates: where the prefilled values came from. */
    private String templateSource;
    /** Published/approved version numbers that depend on this configuration's structure. */
    private String lockedReason;
}
