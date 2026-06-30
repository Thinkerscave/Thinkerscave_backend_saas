package com.thinkerscave.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "Aggregated attendance report")
public class AttendanceSummaryReportResponse {

    private LocalDate fromDate;
    private LocalDate toDate;
    private long totalStudents;
    private double overallPercent;

    private List<ClassSummaryRow> classWiseSummary;
    private List<MonthlyTrendRow> monthlyTrend;
    private List<DefaulterRow> defaulters;

    @Data
    @Builder
    @Schema(description = "Summary row for a class/section")
    public static class ClassSummaryRow {
        private Long classId;
        private String className;
        private Long sectionId;
        private String sectionName;
        private int totalStudents;
        private double avgAttendancePercent;
    }

    @Data
    @Builder
    @Schema(description = "Monthly attendance trend data point")
    public static class MonthlyTrendRow {
        private int year;
        private int month;
        private double avgAttendancePercent;
        private Map<String, Long> statusBreakdown;
    }

    @Data
    @Builder
    @Schema(description = "Student below threshold attendance percentage")
    public static class DefaulterRow {
        private Long studentId;
        private String studentName;
        private String rollNumber;
        private String admissionNumber;
        private String className;
        private String sectionName;
        private int totalDays;
        private int presentDays;
        private double attendancePercent;
    }
}
