package com.thinkerscave.common.attendance.dto;

import com.thinkerscave.common.attendance.domain.Attendance.AttendanceStatus;
import com.thinkerscave.common.attendance.domain.Attendance.AttendanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequestDTO {

    @NotNull(message = "Attendance type is required")
    private AttendanceType attendanceType;

    private Long referenceId;

    @NotBlank(message = "Reference name is required")
    private String referenceName;

    @NotNull(message = "Date is required")
    private LocalDate attendanceDate;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    private Long classId;
    private String className;
    private String sectionName;
    private String shift;
    private String department;
    private String roomNumber;
    private String remarks;
}
