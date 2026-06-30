package com.thinkerscave.attendance.dto.request;

import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Attendance entry for a single student")
public class StudentAttendanceEntry {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    private String studentName;
    private String rollNumber;
    private String admissionNumber;

    @NotNull(message = "Status is required")
    private StudentAttendanceStatus status;

    private String remarks;
}
