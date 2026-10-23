package com.thinkerscave.dashboard.util;

import com.thinkerscave.attendance.dto.response.StaffAttendanceHistoryDayResponse;
import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import com.thinkerscave.dashboard.dto.response.widgetdata.StaffAttendanceWeekData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffAttendanceWeekMapperTest {

    @Test
    void excludesWeekendHolidayLeaveAndFutureDaysFromApplicableDayPercentage() {
        LocalDate monday = LocalDate.of(2026, 10, 12);
        LocalDate friday = monday.plusDays(4);
        StaffAttendanceWeekData summary = StaffAttendanceWeekMapper.map(friday, List.of(
                day(monday, "PRESENT", StaffAttendanceStatus.PRESENT, true),
                day(monday.plusDays(1), "LATE", StaffAttendanceStatus.LATE, true),
                day(monday.plusDays(2), "ABSENT", StaffAttendanceStatus.ABSENT, true),
                day(monday.plusDays(3), "ON_LEAVE", StaffAttendanceStatus.ON_LEAVE, false),
                day(friday, "NOT_STARTED", null, true),
                day(monday.plusDays(5), "WEEKEND", null, false),
                day(monday.plusDays(6), "WEEKEND", null, false)));

        assertEquals(monday, summary.getWeekStart());
        assertEquals(monday.plusDays(6), summary.getWeekEnd());
        assertEquals(4, summary.getApplicableDays());
        assertEquals(2.0, summary.getAttendedDayEquivalent());
        assertEquals(50, summary.getAttendancePercent());
        assertTrue(summary.getDays().get(4).isApplicable());
        assertFalse(summary.getDays().get(3).isApplicable());
    }

    @Test
    void halfDayContributesHalfAnApplicableDay() {
        LocalDate today = LocalDate.of(2026, 10, 12);
        StaffAttendanceWeekData summary = StaffAttendanceWeekMapper.map(today, List.of(
                day(today, "HALF_DAY", StaffAttendanceStatus.HALF_DAY, true)));

        assertEquals(1, summary.getApplicableDays());
        assertEquals(0.5, summary.getAttendedDayEquivalent());
        assertEquals(50, summary.getAttendancePercent());
    }

    private StaffAttendanceHistoryDayResponse day(
            LocalDate date, String displayStatus, StaffAttendanceStatus status, boolean required) {
        return StaffAttendanceHistoryDayResponse.builder()
                .date(date)
                .displayStatus(displayStatus)
                .status(status)
                .attendanceRequired(required)
                .build();
    }
}