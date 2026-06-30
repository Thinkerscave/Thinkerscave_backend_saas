package com.thinkerscave.attendance.dto.request;

import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "Mark staff attendance for a single day")
public class MarkStaffAttendanceRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotNull(message = "Status is required")
    private StaffAttendanceStatus status;

    private LocalDateTime signInTime;
    private LocalDateTime signOutTime;
    private String shift;
    private String remarks;
}
