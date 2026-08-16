package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AcademicYearDashboardResponse {
    private AcademicYearResponse currentYear;
    private AcademicYearResponse upcomingYear;
    private List<AcademicYearResponse> history;
    private long totalYears;
}
