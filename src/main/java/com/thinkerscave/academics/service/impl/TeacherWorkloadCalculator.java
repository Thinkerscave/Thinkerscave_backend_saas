package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.entity.TimetablePeriod;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TeacherWorkloadStatus;
import com.thinkerscave.academics.enums.TimetableSlotKind;

import java.util.List;
import java.util.Map;

/**
 * Pure workload formulas used by the Teacher Workload view.
 *
 * <p>Keeping these rules in one place makes the dashboard, the details modal and the
 * automated tests agree on the same target/maximum/status semantics.</p>
 */
public final class TeacherWorkloadCalculator {

    /** Temporary planning buffer until configurable workload settings exist. */
    public static final int BUFFER_PERCENTAGE = 20;
    public static final double TARGET_RATIO = 0.80;

    /** Temporary near-maximum band: the top 10% of the weekly capacity range. */
    public static final int NEAR_MAXIMUM_BAND_PERCENT = 10;

    private TeacherWorkloadCalculator() {
    }

    /**
     * Expected target = round(capacity × 0.80). Returns {@code null} when no valid
     * capacity exists, so callers never invent a fallback target.
     */
    public static Integer expectedTarget(int weeklyCapacity) {
        if (weeklyCapacity <= 0) {
            return null;
        }
        return (int) Math.round(weeklyCapacity * TARGET_RATIO);
    }

    /** Maximum workload is the full weekly teaching-period capacity. */
    public static Integer maximum(int weeklyCapacity) {
        return weeklyCapacity > 0 ? weeklyCapacity : null;
    }

    /** Remaining capacity may be negative when assigned periods exceed the target. */
    public static Integer remainingCapacity(Integer target, int assignedPeriods) {
        return target == null ? null : target - assignedPeriods;
    }

    /** Overload only happens above the provisional maximum, never above the target. */
    public static int overloadPeriods(int assignedPeriods, Integer maximum) {
        if (maximum == null) {
            return 0;
        }
        return Math.max(0, assignedPeriods - maximum);
    }

    public static Integer nearMaximumThreshold(Integer maximum) {
        if (maximum == null || maximum <= 0) {
            return null;
        }
        int band = (int) Math.round(maximum * (NEAR_MAXIMUM_BAND_PERCENT / 100.0));
        return Math.max(0, maximum - band);
    }

    /**
     * Status rules:
     * <ul>
     *   <li>Overloaded — assigned &gt; maximum</li>
     *   <li>At Target — assigned == expected target</li>
     *   <li>Near Maximum — above target but within the maximum</li>
     *   <li>Below Target — below the expected target</li>
     *   <li>Target Unavailable — no valid schedule pattern</li>
     * </ul>
     */
    public static TeacherWorkloadStatus classify(int assignedPeriods, Integer target, Integer maximum) {
        if (target == null || maximum == null) {
            return TeacherWorkloadStatus.TARGET_UNAVAILABLE;
        }
        if (assignedPeriods > maximum) {
            return TeacherWorkloadStatus.OVERLOADED;
        }
        if (assignedPeriods == target) {
            return TeacherWorkloadStatus.AT_TARGET;
        }
        if (assignedPeriods > target) {
            return TeacherWorkloadStatus.NEAR_MAXIMUM;
        }
        return TeacherWorkloadStatus.BELOW_TARGET;
    }

    /** Weekly teaching capacity for one scheduling unit; breaks and lunch are excluded. */
    public static int weeklyTeachingCapacity(Map<DayOfWeek, List<TimetablePeriod>> scheduleByDay) {
        if (scheduleByDay == null || scheduleByDay.isEmpty()) {
            return 0;
        }
        return scheduleByDay.values().stream()
                .filter(list -> list != null)
                .flatMap(List::stream)
                .filter(period -> period != null && period.getSlotKind() == TimetableSlotKind.TEACHING)
                .mapToInt(period -> 1)
                .sum();
    }
}
