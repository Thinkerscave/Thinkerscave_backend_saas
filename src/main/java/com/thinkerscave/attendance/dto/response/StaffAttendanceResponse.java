package com.thinkerscave.attendance.dto.response;

import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Staff attendance record for a single day")
public class StaffAttendanceResponse {

    private Long attendanceId;
    private Long staffId;
    private String staffName;
    private String staffCode;
    private String department;
    private String designation;
    private LocalDate attendanceDate;
    private LocalDateTime signInTime;
    private LocalDateTime signOutTime;
    private Integer workingMinutes;
    private String shift;
    private StaffAttendanceStatus status;
    private String remarks;
    private String markedBy;
}
