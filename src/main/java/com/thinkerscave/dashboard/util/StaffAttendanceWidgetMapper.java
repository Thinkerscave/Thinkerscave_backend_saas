package com.thinkerscave.dashboard.util;

import com.thinkerscave.attendance.dto.response.StaffAttendanceTodayResponse;
import com.thinkerscave.dashboard.dto.response.widgetdata.StaffAttendanceToggleData;

/**
 * Maps attendance domain responses into the dashboard widget seed payload.
 */
public final class StaffAttendanceWidgetMapper {

    private StaffAttendanceWidgetMapper() {}

    public static StaffAttendanceToggleData toToggleData(StaffAttendanceTodayResponse today) {
        if (today == null) {
            return StaffAttendanceToggleData.builder()
                    .state("NOT_STARTED")
                    .attendanceRequired(true)
                    .signedIn(false)
                    .signedOut(false)
                    .active(false)
                    .autoClosed(false)
                    .workingMinutesSoFar(0)
                    .build();
        }
        boolean signedIn = today.getSignInTime() != null;
        boolean signedOut = today.getSignOutTime() != null;
        return StaffAttendanceToggleData.builder()
                .staffId(today.getStaffId())
                .staffName(today.getStaffName())
                .organizationName(today.getOrganizationName())
                .date(today.getDate())
                .state(today.getState() != null ? today.getState().name() : "NOT_STARTED")
                .status(today.getStatus() != null ? today.getStatus().name() : null)
                .attendanceRequired(today.isAttendanceRequired())
                .attendanceId(today.getAttendanceId())
                .signedIn(signedIn)
                .signedOut(signedOut)
                .active(today.isActive())
                .autoClosed(today.isAutoClosed())
                .signInTime(today.getSignInTime())
                .signOutTime(today.getSignOutTime())
                .workingMinutesSoFar(today.getWorkingMinutes())
                .holidayName(today.getHolidayName())
                .leaveLabel(today.getLeaveLabel())
                .reason(today.getReason())
                .build();
    }
}
