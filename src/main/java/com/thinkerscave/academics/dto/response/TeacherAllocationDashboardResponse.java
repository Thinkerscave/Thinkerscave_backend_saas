package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.TeacherAllocationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class TeacherAllocationDashboardResponse {
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearReadOnly;
    private int maxWeeklyPeriods;
    private boolean maxWeeklyPeriodsFromConfig;

    private long totalSlots;
    private long assignedSlots;
    private long missingSlots;
    private long conflictSlots;

    private List<TeacherAllocationRowResponse> rows;
    private List<TeacherWorkloadResponse> workloads;
}
