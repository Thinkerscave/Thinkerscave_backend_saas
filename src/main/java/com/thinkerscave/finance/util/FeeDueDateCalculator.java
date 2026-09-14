package com.thinkerscave.finance.util;

import com.thinkerscave.finance.enums.FeeFrequency;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Due-date and period calculation. structure.dueDay (1–28) is authoritative;
 * invalid calendar days are clamped to the last day of the month.
 */
public final class FeeDueDateCalculator {

    private FeeDueDateCalculator() {}

    public record PeriodSpec(
            String periodKey,
            String periodLabel,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate dueDate
    ) {}

    public static LocalDate dueDate(LocalDate periodStart, int dueDay) {
        YearMonth ym = YearMonth.from(periodStart);
        int day = Math.min(Math.max(dueDay, 1), ym.lengthOfMonth());
        return ym.atDay(day);
    }

    public static List<PeriodSpec> periodsForYear(
            FeeFrequency frequency,
            LocalDate yearStart,
            LocalDate yearEnd,
            int dueDay
    ) {
        List<PeriodSpec> out = new ArrayList<>();
        switch (frequency) {
            case ONE_TIME -> {
                // Exactly one period per student per academic year.
                // Academic-year start is the period start/end basis; dueDay is authoritative.
                LocalDate start = yearStart;
                out.add(new PeriodSpec(
                        "ONE_TIME",
                        "One Time",
                        start,
                        start,
                        dueDate(start, dueDay)
                ));
            }
            case YEARLY -> {
                out.add(new PeriodSpec(
                        "YEARLY",
                        "Yearly",
                        yearStart,
                        yearEnd,
                        dueDate(yearStart, dueDay)
                ));
            }
            case MONTHLY -> {
                YearMonth cursor = YearMonth.from(yearStart);
                YearMonth last = YearMonth.from(yearEnd);
                while (!cursor.isAfter(last)) {
                    LocalDate start = cursor.atDay(1);
                    if (start.isBefore(yearStart)) start = yearStart;
                    LocalDate end = cursor.atEndOfMonth();
                    if (end.isAfter(yearEnd)) end = yearEnd;
                    String key = String.format("%04d-%02d", cursor.getYear(), cursor.getMonthValue());
                    String label = cursor.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + cursor.getYear();
                    out.add(new PeriodSpec(key, label, start, end, dueDate(cursor.atDay(1), dueDay)));
                    cursor = cursor.plusMonths(1);
                }
            }
            case QUARTERLY -> {
                LocalDate cursor = yearStart;
                int q = 1;
                while (!cursor.isAfter(yearEnd) && q <= 4) {
                    LocalDate end = cursor.plusMonths(3).minusDays(1);
                    if (end.isAfter(yearEnd)) end = yearEnd;
                    String key = yearStart.getYear() + "-Q" + q;
                    out.add(new PeriodSpec(key, "Q" + q + " " + yearStart.getYear(), cursor, end, dueDate(cursor, dueDay)));
                    cursor = end.plusDays(1);
                    q++;
                }
            }
            case HALF_YEARLY -> {
                LocalDate h1Start = yearStart;
                LocalDate h1End = yearStart.plusMonths(6).minusDays(1);
                if (h1End.isAfter(yearEnd)) h1End = yearEnd;
                out.add(new PeriodSpec(yearStart.getYear() + "-H1", "H1 " + yearStart.getYear(),
                        h1Start, h1End, dueDate(h1Start, dueDay)));
                LocalDate h2Start = h1End.plusDays(1);
                if (!h2Start.isAfter(yearEnd)) {
                    out.add(new PeriodSpec(yearStart.getYear() + "-H2", "H2 " + yearStart.getYear(),
                            h2Start, yearEnd, dueDate(h2Start, dueDay)));
                }
            }
        }
        return out;
    }
}
