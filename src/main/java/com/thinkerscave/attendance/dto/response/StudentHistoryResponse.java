package com.thinkerscave.attendance.dto.response;

import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@Schema(description = "Attendance history for an individual student")
public class StudentHistoryResponse {

    private Long studentId;
    private String studentName;
    private String rollNumber;
    private String admissionNumber;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private int totalDays;
    private int presentDays;
    private int absentDays;
    private int lateDays;
    private int excusedDays;
    private double attendancePercent;
    private List<DayRecord> records;

    @Data
    @Builder
    @Schema(description = "Attendance record for a single day")
    public static class DayRecord {
        private LocalDate date;
        private StudentAttendanceStatus status;
        private String remarks;
    }
}
