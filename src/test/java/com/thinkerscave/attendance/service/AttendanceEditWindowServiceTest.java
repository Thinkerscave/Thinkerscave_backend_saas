package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.enums.AttendanceEditingPolicy;
import com.thinkerscave.attendance.service.AttendanceEditWindowService.EditWindowDecision;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceEditWindowServiceTest {

    // 2026-03-04 is a Wednesday; its Monday-start week runs 2026-03-02 .. 2026-03-08.
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 3, 4);

    @Test
    void sameDay_allowsOnlyToday() {
        assertTrue(AttendanceEditWindowService.evaluate(WEDNESDAY, WEDNESDAY, AttendanceEditingPolicy.SAME_DAY).editable());
        assertFalse(AttendanceEditWindowService.evaluate(WEDNESDAY.minusDays(1), WEDNESDAY, AttendanceEditingPolicy.SAME_DAY).editable());
    }

    @Test
    void sameWeek_allowsMondayToSundayOfCurrentWeek() {
        LocalDate monday = LocalDate.of(2026, 3, 2);
        LocalDate sunday = LocalDate.of(2026, 3, 8);
        assertTrue(AttendanceEditWindowService.evaluate(monday, WEDNESDAY, AttendanceEditingPolicy.SAME_WEEK).editable());
        assertTrue(AttendanceEditWindowService.evaluate(WEDNESDAY, WEDNESDAY, AttendanceEditingPolicy.SAME_WEEK).editable());
        assertTrue(AttendanceEditWindowService.evaluate(sunday, sunday, AttendanceEditingPolicy.SAME_WEEK).editable());
    }

    @Test
    void sameWeek_deniesPreviousSundayAcrossMondayBoundary() {
        LocalDate previousSunday = LocalDate.of(2026, 3, 1);
        EditWindowDecision decision = AttendanceEditWindowService.evaluate(previousSunday, WEDNESDAY, AttendanceEditingPolicy.SAME_WEEK);
        assertFalse(decision.editable());
        assertNotNull(decision.reason());
    }

    @Test
    void sameMonth_allowsDatesInCurrentMonthOnly() {
        assertTrue(AttendanceEditWindowService.evaluate(LocalDate.of(2026, 3, 1), WEDNESDAY, AttendanceEditingPolicy.SAME_MONTH).editable());
        assertFalse(AttendanceEditWindowService.evaluate(LocalDate.of(2026, 2, 28), WEDNESDAY, AttendanceEditingPolicy.SAME_MONTH).editable());
    }

    @Test
    void futureDates_areAlwaysDenied() {
        LocalDate tomorrow = WEDNESDAY.plusDays(1);
        for (AttendanceEditingPolicy policy : AttendanceEditingPolicy.values()) {
            EditWindowDecision decision = AttendanceEditWindowService.evaluate(tomorrow, WEDNESDAY, policy);
            assertFalse(decision.editable(), policy.name());
            assertNotNull(decision.reason());
        }
    }

    @Test
    void allowedDecision_hasNoReason() {
        EditWindowDecision decision = AttendanceEditWindowService.evaluate(WEDNESDAY, WEDNESDAY, AttendanceEditingPolicy.SAME_DAY);
        assertNull(decision.reason());
    }
}
