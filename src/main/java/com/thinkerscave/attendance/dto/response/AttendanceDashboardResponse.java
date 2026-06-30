package com.thinkerscave.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Attendance module dashboard statistics")
public class AttendanceDashboardResponse {

    // Student stats
    private long studentsPresent;
    private long studentsAbsent;
    private long studentsLate;
    private long pendingClassCount;
    private double studentOverallPercent;

    // Staff stats
    private long staffPresent;
    private long staffAbsent;
    private long staffLate;
    private long staffOnLeave;

    // Pending tasks
    private List<PendingClassInfo> pendingClasses;

    @Data
    @Builder
    @Schema(description = "Class/section that has not been marked today")
    public static class PendingClassInfo {
        private Long classId;
        private String className;
        private Long sectionId;
        private String sectionName;
    }
}
