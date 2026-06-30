package com.thinkerscave.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Staff attendance summary for a date range")
public class StaffAttendanceSummaryResponse {

    private Long staffId;
    private String staffName;
    private String staffCode;
    private String department;
    private int totalDays;
    private int presentDays;
    private int absentDays;
    private int lateDays;
    private int onLeaveDays;
    private double attendancePercent;
    private double avgWorkingHours;
}
