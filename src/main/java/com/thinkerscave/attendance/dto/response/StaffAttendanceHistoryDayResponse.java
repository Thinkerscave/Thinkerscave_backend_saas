package com.thinkerscave.attendance.dto.response;

import com.thinkerscave.attendance.enums.RegularizationRequestStatus;
import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One day in the staff member's attendance history calendar/list.
 */
@Data
@Builder
public class StaffAttendanceHistoryDayResponse {
    private LocalDate date;
    private String dayOfWeek;
    /** Display status for calendar: PRESENT, LATE, ABSENT, ON_LEAVE, WEEKEND, HOLIDAY, NOT_STARTED */
    private String displayStatus;
    private StaffAttendanceStatus status;
    private Long attendanceId;
    private LocalDateTime signInTime;
    private LocalDateTime signOutTime;
    private Integer workingMinutes;
    private boolean attendanceRequired;
    private boolean autoClosed;
    private String holidayName;
    private String leaveLabel;
    private RegularizationRequestStatus regularizationStatus;
    private Long regularizationRequestId;
    private boolean canRegularize;
}
