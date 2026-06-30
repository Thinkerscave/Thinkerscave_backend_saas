package com.thinkerscave.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@Schema(description = "Class-level attendance summary for a given date")
public class ClassAttendanceSummaryResponse {

    private LocalDate attendanceDate;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private int totalStudents;
    private int presentCount;
    private int absentCount;
    private int lateCount;
    private int excusedCount;
    private double attendancePercent;
    private List<StudentAttendanceResponse> students;
}
