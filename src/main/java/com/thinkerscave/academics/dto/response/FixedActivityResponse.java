package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
public class FixedActivityResponse {

    private Long fixedActivityId;
    private Long academicYearId;
    private String name;
    private String description;
    private String activityType;
    private Boolean appliesToAllGroups;
    private LocalTime startTime;
    private LocalTime endTime;
    private String recurrence;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private Set<DayOfWeek> days;
    private List<Group> groups;

    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    /** ACADEMIC_SUBJECT, PRACTICAL_LAB or ACTIVITY. */
    private String subjectType;
    private String periodName;
    private Long staffId;
    private String staffName;
    /** Resolved configured period for every applicable group/section/day. */
    private List<Period> periods;

    @Getter
    @Setter
    @Builder
    public static class Group {
        private Long academicGroupId;
        private String academicGroupName;
        private Long sectionId;
        private String sectionName;
    }

    @Getter
    @Setter
    @Builder
    public static class Period {
        private Long timetablePeriodId;
        private Long academicGroupId;
        private Long sectionId;
        private DayOfWeek dayOfWeek;
        private String periodName;
        private Integer periodNumber;
        private LocalTime startTime;
        private LocalTime endTime;
        private Long schedulePatternId;
        private String schedulePatternName;
    }
}
