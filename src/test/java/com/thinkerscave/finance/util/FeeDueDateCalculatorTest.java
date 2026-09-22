package com.thinkerscave.finance.util;

import com.thinkerscave.finance.enums.FeeFrequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeeDueDateCalculatorTest {

    private static final LocalDate YEAR_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate YEAR_END = LocalDate.of(2027, 3, 31);

    @Test
    void oneTimeProducesSinglePeriodKeyedToAcademicYearStart() {
        List<FeeDueDateCalculator.PeriodSpec> specs = FeeDueDateCalculator.periodsForYear(
                FeeFrequency.ONE_TIME, YEAR_START, YEAR_END, 10);
        assertEquals(1, specs.size());
        assertEquals("ONE_TIME", specs.get(0).periodKey());
        assertEquals(YEAR_START, specs.get(0).periodStart());
        assertEquals(YEAR_START, specs.get(0).periodEnd());
        assertEquals(LocalDate.of(2026, 4, 10), specs.get(0).dueDate());
    }

    @Test
    void monthlyUsesMonthKeyAndStructureDueDay() {
        List<FeeDueDateCalculator.PeriodSpec> specs = FeeDueDateCalculator.periodsForYear(
                FeeFrequency.MONTHLY, YEAR_START, LocalDate.of(2026, 6, 30), 10);
        assertEquals(3, specs.size());
        assertEquals("2026-04", specs.get(0).periodKey());
        assertEquals(LocalDate.of(2026, 4, 10), specs.get(0).dueDate());
        assertEquals("2026-05", specs.get(1).periodKey());
        assertEquals(LocalDate.of(2026, 5, 10), specs.get(1).dueDate());
    }

    @Test
    void quarterlyAlignedToAcademicYearStart() {
        List<FeeDueDateCalculator.PeriodSpec> specs = FeeDueDateCalculator.periodsForYear(
                FeeFrequency.QUARTERLY, YEAR_START, YEAR_END, 15);
        assertFalse(specs.isEmpty());
        assertEquals("2026-Q1", specs.get(0).periodKey());
        assertEquals(YEAR_START, specs.get(0).periodStart());
        assertEquals(LocalDate.of(2026, 4, 15), specs.get(0).dueDate());
    }

    @Test
    void halfYearlyProducesH1AndH2() {
        List<FeeDueDateCalculator.PeriodSpec> specs = FeeDueDateCalculator.periodsForYear(
                FeeFrequency.HALF_YEARLY, YEAR_START, YEAR_END, 10);
        assertEquals(2, specs.size());
        assertEquals("2026-H1", specs.get(0).periodKey());
        assertEquals(LocalDate.of(2026, 4, 10), specs.get(0).dueDate());
        assertEquals("2026-H2", specs.get(1).periodKey());
        assertEquals(specs.get(0).periodEnd().plusDays(1), specs.get(1).periodStart());
    }

    @Test
    void yearlyDueOnAcademicYearStartMonthDueDay() {
        List<FeeDueDateCalculator.PeriodSpec> specs = FeeDueDateCalculator.periodsForYear(
                FeeFrequency.YEARLY, YEAR_START, YEAR_END, 10);
        assertEquals(1, specs.size());
        assertEquals("YEARLY", specs.get(0).periodKey());
        assertEquals(YEAR_START, specs.get(0).periodStart());
        assertEquals(YEAR_END, specs.get(0).periodEnd());
        assertEquals(LocalDate.of(2026, 4, 10), specs.get(0).dueDate());
    }

    @Test
    void dueDayClampedToLastDayOfShortMonth() {
        // February 2027 has 28 days; dueDay 28 is valid. Use a leap-year Feb for clamp proof via helper.
        LocalDate due = FeeDueDateCalculator.dueDate(LocalDate.of(2025, 2, 1), 31);
        assertEquals(LocalDate.of(2025, 2, 28), due);
        LocalDate leap = FeeDueDateCalculator.dueDate(LocalDate.of(2024, 2, 1), 31);
        assertEquals(LocalDate.of(2024, 2, 29), leap);
    }
}
