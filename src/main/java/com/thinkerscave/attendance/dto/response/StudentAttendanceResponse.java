package com.thinkerscave.attendance.dto.response;

import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Student attendance record for a single day")
public class StudentAttendanceResponse {

    private Long attendanceId;
    private Long studentId;
    private String studentName;
    private String rollNumber;
    private String admissionNumber;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private LocalDate attendanceDate;
    private StudentAttendanceStatus status;
    private String remarks;
    private String markedBy;
}
