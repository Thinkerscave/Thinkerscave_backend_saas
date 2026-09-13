package com.thinkerscave.admission.scheduling;

import com.thinkerscave.attendance.entity.AttendanceSetting;
import com.thinkerscave.attendance.repository.AttendanceSettingRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * Plans the first follow-up slot after counselor assignment.
 * <p>
 * Rules (SLA-first, capacity-aware):
 * <ul>
 *   <li>Prefer same business day when the counselor's load is among the lightest options</li>
 *   <li>Otherwise prefer the next lighter business day</li>
 *   <li>Never schedule beyond 2 business days from now</li>
 * </ul>
 * Business hours come from the org's attendance window when configured; otherwise
 * standard office hours. Working days are Mon–Fri.
 */
@Component
@RequiredArgsConstructor
public class FollowUpSlotPlanner {

    private static final Set<DayOfWeek> WEEKEND = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final LocalTime DEFAULT_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(17, 0);
    private static final int MAX_BUSINESS_DAY_OFFSET = 2;

    private final AttendanceSettingRepository attendanceSettingRepository;

    /**
     * @param loadForDate returns how many scheduled follow-ups the counselor already has on that date
     */
    public LocalDateTime suggestFirstFollowUpAt(ToLongFunction<LocalDate> loadForDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime start = businessStart();
        LocalTime end = businessEnd();
        if (!end.isAfter(start)) {
            end = DEFAULT_END;
            start = DEFAULT_START;
        }

        List<LocalDate> candidates = businessDaysWithin(now.toLocalDate(), MAX_BUSINESS_DAY_OFFSET);
        if (candidates.isEmpty()) {
            return alignToBusinessHours(now.plusDays(1).with(start), start, end);
        }

        long minLoad = Long.MAX_VALUE;
        for (LocalDate day : candidates) {
            minLoad = Math.min(minLoad, Math.max(0L, loadForDate.applyAsLong(day)));
        }

        LocalDate chosen = candidates.get(0);
        for (LocalDate day : candidates) {
            long load = Math.max(0L, loadForDate.applyAsLong(day));
            if (load <= minLoad) {
                chosen = day;
                break;
            }
        }

        LocalDateTime slot;
        if (chosen.equals(now.toLocalDate())) {
            LocalDateTime soonest = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
            if (soonest.toLocalTime().isBefore(start)) {
                slot = LocalDateTime.of(chosen, start);
            } else if (!soonest.toLocalTime().isBefore(end)) {
                // Remaining same-day capacity is past close — fall through to next candidate if any.
                LocalDate fallback = candidates.size() > 1 ? candidates.get(1) : chosen;
                slot = LocalDateTime.of(fallback, start);
            } else {
                slot = soonest;
            }
        } else {
            slot = LocalDateTime.of(chosen, start);
        }
        return alignToBusinessHours(slot, start, end);
    }

    private List<LocalDate> businessDaysWithin(LocalDate from, int maxOffsetInclusive) {
        List<LocalDate> days = new ArrayList<>(maxOffsetInclusive + 1);
        LocalDate cursor = from;
        int guard = 0;
        while (days.size() <= maxOffsetInclusive && guard++ < 21) {
            if (isBusinessDay(cursor)) {
                days.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    private boolean isBusinessDay(LocalDate date) {
        return !WEEKEND.contains(date.getDayOfWeek());
    }

    private LocalDateTime alignToBusinessHours(LocalDateTime value, LocalTime start, LocalTime end) {
        LocalDate date = value.toLocalDate();
        if (!isBusinessDay(date)) {
            date = nextBusinessDay(date);
            return LocalDateTime.of(date, start);
        }
        LocalTime time = value.toLocalTime();
        if (time.isBefore(start)) {
            return LocalDateTime.of(date, start);
        }
        if (!time.isBefore(end)) {
            return LocalDateTime.of(nextBusinessDay(date), start);
        }
        return value.withSecond(0).withNano(0);
    }

    private LocalDate nextBusinessDay(LocalDate date) {
        LocalDate cursor = date.plusDays(1);
        while (!isBusinessDay(cursor)) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }

    private LocalTime businessStart() {
        return resolveWindow().start();
    }

    private LocalTime businessEnd() {
        return resolveWindow().end();
    }

    private Window resolveWindow() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId != null) {
            AttendanceSetting setting = attendanceSettingRepository.findByOrganizationId(orgId).orElse(null);
            if (setting != null && setting.getWindowStartTime() != null && setting.getWindowEndTime() != null) {
                return new Window(setting.getWindowStartTime(), setting.getWindowEndTime());
            }
        }
        return new Window(DEFAULT_START, DEFAULT_END);
    }

    private record Window(LocalTime start, LocalTime end) {}
}
