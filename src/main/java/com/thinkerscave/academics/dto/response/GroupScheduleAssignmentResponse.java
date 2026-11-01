package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class GroupScheduleAssignmentResponse {

    private Long groupScheduleAssignmentId;
    private Long academicGroupId;
    private String academicGroupName;
    private String academicGroupCode;
    private Long sectionId;
    private String sectionName;
    private Long schedulePatternId;
    private String schedulePatternName;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
}
