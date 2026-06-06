package com.thinkerscave.common.student.workspace.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentKpiResponse {
    private long totalStudents;
    private long activeStudents;
    private long inactiveStudents;
    private long newAdmissionsThisYear;
    private long alumniCount;
}
