package com.thinkerscave.attendance.dto.response;

import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import com.thinkerscave.attendance.enums.StaffAttendanceWidgetState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Authoritative "today" attendance payload for the Staff Attendance dashboard widget.
 */
@Data
@Builder
@Schema(description = "Today's staff attendance widget state")
public class StaffAttendanceTodayResponse {

    private Long attendanceId;
    private Long staffId;
    private String staffName;
    private String organizationName;

    private LocalDate date;
    private StaffAttendanceWidgetState state;
    private StaffAttendanceStatus status;

    private boolean attendanceRequired;
    private boolean active;
    private boolean autoClosed;

    private LocalDateTime signInTime;
    private LocalDateTime signOutTime;
    private Integer workingMinutes;

    /** Holiday title when state is HOLIDAY. */
    private String holidayName;

    /** Leave label when state is ON_LEAVE. */
    private String leaveLabel;

    /** Machine reason for non-working / blocked states (APPROVED_LEAVE, HOLIDAY, WEEKEND, …). */
    private String reason;
}
