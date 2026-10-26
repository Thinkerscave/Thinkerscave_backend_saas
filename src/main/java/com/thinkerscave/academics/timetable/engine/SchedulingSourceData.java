package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SchedulingSourceData {

    private final TimetableConfiguration configuration;
    private final List<DayOfWeek> workingDays;
    private final List<TimetablePeriod> teachingPeriods;
    private final List<TimetablePeriod> allPeriods;
    private final Map<Long, Map<DayOfWeek, List<TimetablePeriod>>> schedulesByUnit;
    private final int maxTeacherWeeklyPeriods;
    /** Number of teaching periods that belong to the first half of the day. */
    private final int halfIndex;

    public int workingDayCount() {
        return Math.max(workingDays.size(), 1);
    }

    public List<TimetablePeriod> slotsForUnitOnDay(Long unitId, DayOfWeek day) {
        Map<DayOfWeek, List<TimetablePeriod>> byDay = schedulesByUnit.get(unitId);
        if (byDay == null) return List.of();
        return byDay.getOrDefault(day, List.of()).stream()
                .filter(s -> s.getSlotKind() == com.thinkerscave.academics.enums.TimetableSlotKind.TEACHING)
                .toList();
    }

    public boolean isSlotAllowed(Long unitId, DayOfWeek day, Long periodId) {
        return slotsForUnitOnDay(unitId, day).stream()
                .anyMatch(s -> s.getTimetablePeriodId().equals(periodId));
    }
}
