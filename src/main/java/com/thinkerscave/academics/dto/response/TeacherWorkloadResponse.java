package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TeacherWorkloadResponse {
    private Long staffId;
    private Long academicYearId;
    private Integer assignedWeeklyPeriods;
    private Integer maxWeeklyPeriods;
    private String status;
}
