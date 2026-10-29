package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.entity.TimetablePeriod;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TeacherWorkloadStatus;
import com.thinkerscave.academics.enums.TimetableSlotKind;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TeacherWorkloadCalculatorTest {

    @Test
    void calculatesTargetFromEightyPercentOfCapacity() {
        assertEquals(38, TeacherWorkloadCalculator.expectedTarget(48));
        assertEquals(48, TeacherWorkloadCalculator.maximum(48));
        assertEquals(6, TeacherWorkloadCalculator.remainingCapacity(38, 32));
        assertEquals(0, TeacherWorkloadCalculator.overloadPeriods(32, 48));
    }

    @Test
    void remainingCapacityIsNegativeAboveTargetButNotOverloadedWithinMaximum() {
        assertEquals(-4, TeacherWorkloadCalculator.remainingCapacity(38, 42));
        assertEquals(0, TeacherWorkloadCalculator.overloadPeriods(42, 48));
        assertEquals(TeacherWorkloadStatus.NEAR_MAXIMUM,
                TeacherWorkloadCalculator.classify(42, 38, 48));
    }

    @Test
    void classifiesEveryStatusConsistently() {
        assertEquals(TeacherWorkloadStatus.OVERLOADED,
                TeacherWorkloadCalculator.classify(49, 38, 48));
        assertEquals(TeacherWorkloadStatus.NEAR_MAXIMUM,
                TeacherWorkloadCalculator.classify(39, 38, 48));
        assertEquals(TeacherWorkloadStatus.AT_TARGET,
                TeacherWorkloadCalculator.classify(38, 38, 48));
        assertEquals(TeacherWorkloadStatus.BELOW_TARGET,
                TeacherWorkloadCalculator.classify(37, 38, 48));
        assertEquals(TeacherWorkloadStatus.TARGET_UNAVAILABLE,
                TeacherWorkloadCalculator.classify(20, null, null));
    }

    @Test
    void returnsNoFallbackWhenCapacityIsMissing() {
        assertNull(TeacherWorkloadCalculator.expectedTarget(0));
        assertNull(TeacherWorkloadCalculator.maximum(0));
        assertNull(TeacherWorkloadCalculator.remainingCapacity(null, 20));
        assertNull(TeacherWorkloadCalculator.nearMaximumThreshold(null));
    }

    @Test
    void usesTopTenPercentOfCapacityAsNearMaximumBand() {
        assertEquals(43, TeacherWorkloadCalculator.nearMaximumThreshold(48));
        assertEquals(36, TeacherWorkloadCalculator.nearMaximumThreshold(40));
    }

    @Test
    void countsOnlyTeachingSlotsAsCapacity() {
        Map<DayOfWeek, List<TimetablePeriod>> schedule = new LinkedHashMap<>();
        schedule.put(DayOfWeek.MONDAY, List.of(
                period(1, TimetableSlotKind.TEACHING),
                period(2, TimetableSlotKind.TEACHING),
                period(3, TimetableSlotKind.BREAK)));
        schedule.put(DayOfWeek.TUESDAY, List.of(
                period(4, TimetableSlotKind.TEACHING),
                period(5, TimetableSlotKind.BREAK)));

        assertEquals(3, TeacherWorkloadCalculator.weeklyTeachingCapacity(schedule));
        assertEquals(0, TeacherWorkloadCalculator.weeklyTeachingCapacity(Map.of()));
    }

    private TimetablePeriod period(int number, TimetableSlotKind kind) {
        TimetablePeriod period = new TimetablePeriod();
        period.setPeriodNumber((short) number);
        period.setSlotKind(kind);
        return period;
    }
}
