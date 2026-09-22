package com.thinkerscave.attendance.dto.response;

import com.thinkerscave.attendance.enums.RegularizationRequestStatus;
import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class RegularizationRequestResponse {
    private Long requestId;
    private Long staffId;
    private String staffName;
    private String department;
    private Long attendanceId;
    private LocalDate attendanceDate;
    private StaffAttendanceStatus requestedStatus;
    private LocalDateTime requestedSignInTime;
    private LocalDateTime requestedSignOutTime;
    private Integer requestedWorkingMinutes;
    private String reason;
    private String remarks;
    private RegularizationRequestStatus status;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private String decisionBy;
    private LocalDateTime decisionAt;
    private String decisionComment;

    private StaffAttendanceStatus previousStatus;
    private LocalDateTime previousSignInTime;
    private LocalDateTime previousSignOutTime;
    private Integer previousWorkingMinutes;

    private StaffAttendanceStatus currentStatus;
    private LocalDateTime currentSignInTime;
    private LocalDateTime currentSignOutTime;
    private Integer currentWorkingMinutes;
}
