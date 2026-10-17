package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.enums.AttendanceEditingPolicy;
import com.thinkerscave.settings.ModuleSettingCatalog;
import com.thinkerscave.settings.ModuleSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

/**
 * Enforces the organization's {@code ATTENDANCE_EDITING_POLICY} for attendance writes.
 *
 * <p>A date is writable when it is not in the future and falls inside the policy window
 * that contains today:
 * <ul>
 *   <li>{@code SAME_DAY} - only today.</li>
 *   <li>{@code SAME_WEEK} - the Monday-to-Sunday week that contains today.</li>
 *   <li>{@code SAME_MONTH} - the calendar month that contains today.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AttendanceEditWindowService {

    private final ModuleSettingsService moduleSettingsService;

    public AttendanceEditingPolicy policy(Long orgId) {
        return AttendanceEditingPolicy.valueOf(moduleSettingsService.getString(
                orgId, ModuleSettingCatalog.ATTENDANCE, ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY));
    }

    public EditWindowDecision evaluate(Long orgId, LocalDate date) {
        return evaluate(date, LocalDate.now(), policy(orgId));
    }

    /** Pure rule evaluation, independent of settings and clock. */
    public static EditWindowDecision evaluate(LocalDate date, LocalDate today, AttendanceEditingPolicy policy) {
        if (date.isAfter(today)) {
            return EditWindowDecision.denied("Attendance cannot be recorded for a future date: " + date + ".");
        }
        boolean withinWindow = switch (policy) {
            case SAME_DAY -> date.equals(today);
            case SAME_WEEK -> date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .equals(today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)));
            case SAME_MONTH -> YearMonth.from(date).equals(YearMonth.from(today));
        };
        if (withinWindow) {
            return EditWindowDecision.allowed();
        }
        return EditWindowDecision.denied(
                "Attendance for " + date + " is outside the editing window (" + describe(policy) + ").");
    }

    private static String describe(AttendanceEditingPolicy policy) {
        return switch (policy) {
            case SAME_DAY -> "same day";
            case SAME_WEEK -> "same week";
            case SAME_MONTH -> "same month";
        };
    }

    public record EditWindowDecision(boolean editable, String reason) {
        static EditWindowDecision allowed() {
            return new EditWindowDecision(true, null);
        }

        static EditWindowDecision denied(String reason) {
            return new EditWindowDecision(false, reason);
        }
    }
}
