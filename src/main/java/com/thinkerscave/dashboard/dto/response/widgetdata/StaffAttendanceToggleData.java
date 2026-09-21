package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seed payload for the reusable Staff Attendance dashboard widget.
 * Mirrors {@code StaffAttendanceTodayResponse} so the UI can render from
 * workspace data and revalidate via {@code GET /me/today}.
 */
@Data
@Builder
public class StaffAttendanceToggleData {
    private Long staffId;
    private String staffName;
    private String organizationName;
    private LocalDate date;
    private String state;
    private String status;
    private boolean attendanceRequired;
    private Long attendanceId;
    private boolean signedIn;
    private boolean signedOut;
    private boolean active;
    private boolean autoClosed;
    private LocalDateTime signInTime;
    private LocalDateTime signOutTime;
    private Integer workingMinutesSoFar;
    private String holidayName;
    private String leaveLabel;
    private String reason;
}
