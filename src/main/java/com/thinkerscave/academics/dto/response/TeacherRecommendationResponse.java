package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TeacherRecommendationResponse {
    private Long staffId;
    private String staffName;
    private Integer assignedWeeklyPeriods;
    private Integer maxWeeklyPeriods;
    private String workloadStatus;
    private boolean recommended;
    private String reason;
}
