package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

/** Server-resolved schedule options and validation for a fixed activity form. */
@Getter
@Setter
@Builder
public class FixedActivityScheduleResponse {

    private Long academicYearId;
    private List<Combination> combinations;
    private List<PeriodOption> commonPeriods;
    private List<TeacherOption> teachers;
    private List<ValidationIssue> issues;

    @Getter
    @Setter
    @Builder
    public static class Combination {
        private Long academicGroupId;
        private String academicGroupName;
        private Long sectionId;
        private String sectionName;
        private String unitLabel;
        private String dayOfWeek;
        private Long schedulePatternId;
        private String schedulePatternName;
        private List<Period> periods;
    }

    @Getter
    @Setter
    @Builder
    public static class Period {
        private Long timetablePeriodId;
        private Integer periodNumber;
        private String name;
        private LocalTime startTime;
        private LocalTime endTime;
    }

    @Getter
    @Setter
    @Builder
    public static class PeriodOption {
        private String name;
        /** Human-readable time when every schedule agrees, otherwise a short "varies" label. */
        private String displayTime;
        private boolean timesVary;
        private List<String> times;
    }

    @Getter
    @Setter
    @Builder
    public static class TeacherOption {
        private Long staffId;
        private String staffName;
        /** True when the teacher is allocated for every selected unit. */
        private boolean eligibleForAllUnits;
        private List<String> unitLabels;
    }

    @Getter
    @Setter
    @Builder
    public static class ValidationIssue {
        private String code;
        private String message;
        private Long academicGroupId;
        private Long sectionId;
        private String dayOfWeek;
    }
}
