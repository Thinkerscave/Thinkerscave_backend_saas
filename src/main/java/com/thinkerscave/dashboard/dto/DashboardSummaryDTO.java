package com.thinkerscave.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Aggregated KPI summary for the current organization's dashboard.
 */
@Data
@Builder
public class DashboardSummaryDTO {

    // Students
    private long totalStudents;
    private long activeStudents;

    // Staff
    private long totalStaff;
    private long activeStaff;

    // Attendance (today)
    private long todayStudentAttendancePresent;
    private long todayStudentAttendanceAbsent;

    // Admission pipeline
    private long openInquiries;
    private long pendingApplications;
    private long newInquiriesToday;

    // Users
    private long activeUsers;

    private LocalDate reportDate;
}
