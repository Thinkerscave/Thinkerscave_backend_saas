package com.thinkerscave.dashboard.util;

import com.thinkerscave.attendance.dto.response.StaffAttendanceHistoryDayResponse;
import com.thinkerscave.dashboard.dto.response.widgetdata.StaffAttendanceWeekData;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StaffAttendanceWeekMapper {

    private StaffAttendanceWeekMapper() {}

    public static StaffAttendanceWeekData map(
            LocalDate today, List<StaffAttendanceHistoryDayResponse> history) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<LocalDate, StaffAttendanceHistoryDayResponse> byDate = history.stream()
                .collect(Collectors.toMap(StaffAttendanceHistoryDayResponse::getDate, Function.identity(), (first, ignored) -> first));

        List<StaffAttendanceWeekData.Day> days = Stream.iterate(weekStart, date -> date.plusDays(1))
                .limit(7)
                .map(date -> day(date, today, byDate.get(date)))
                .toList();
        int applicableDays = (int) days.stream().filter(StaffAttendanceWeekData.Day::isApplicable).count();
        double attendedDayEquivalent = days.stream()
                .filter(StaffAttendanceWeekData.Day::isApplicable)
                .mapToDouble(day -> attendanceCredit(day.getDisplayStatus()))
                .sum();
        int percentage = applicableDays == 0 ? 0
                : (int) Math.round(attendedDayEquivalent * 100.0 / applicableDays);

        return StaffAttendanceWeekData.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .attendedDayEquivalent(attendedDayEquivalent)
                .applicableDays(applicableDays)
                .attendancePercent(percentage)
                .days(days)
                .build();
    }

    private static StaffAttendanceWeekData.Day day(
            LocalDate date, LocalDate today, StaffAttendanceHistoryDayResponse historyDay) {
        String status = historyDay == null
                ? date.isAfter(today) ? "FUTURE" : "NOT_RECORDED"
                : historyDay.getDisplayStatus();
        boolean applicable = historyDay != null
                && historyDay.isAttendanceRequired()
                && !date.isAfter(today);
        return StaffAttendanceWeekData.Day.builder()
                .date(date)
                .dayLabel(date.getDayOfWeek().name())
                .displayStatus(status)
                .applicable(applicable)
                .build();
    }

    private static double attendanceCredit(String status) {
        if ("PRESENT".equals(status) || "LATE".equals(status) || "WFH".equals(status)) return 1.0;
        if ("HALF_DAY".equals(status)) return 0.5;
        return 0.0;
    }
}