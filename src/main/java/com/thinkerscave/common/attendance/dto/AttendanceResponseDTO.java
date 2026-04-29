package com.thinkerscave.common.attendance.dto;

import com.thinkerscave.common.attendance.domain.Attendance.AttendanceStatus;
import com.thinkerscave.common.attendance.domain.Attendance.AttendanceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceResponseDTO {
    private Long id;
    private Long organizationId;
    private AttendanceType attendanceType;
    private Long referenceId;
    private String referenceName;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private Long classId;
    private String className;
    private String sectionName;
    private String shift;
    private String department;
    private String roomNumber;
    private String remarks;
    private String markedBy;
    private LocalDateTime createdAt;
}
