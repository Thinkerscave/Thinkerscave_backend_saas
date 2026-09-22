package com.thinkerscave.attendance.enums;

/**
 * Dashboard widget state for today's staff attendance.
 * Distinct from {@link StaffAttendanceStatus}, which is the recorded attendance outcome.
 */
public enum StaffAttendanceWidgetState {
    NOT_STARTED,
    ACTIVE,
    COMPLETED,
    WEEKEND,
    HOLIDAY,
    ON_LEAVE
}
