package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class AcademicScheduleResponse {
    private Long scheduleId;
    private Long academicYearId;
    private String yearCode;
    private String scheduleName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private String remarks;
}
