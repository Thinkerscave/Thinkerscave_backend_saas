package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class StaffAttendanceWeekData {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private double attendedDayEquivalent;
    private int applicableDays;
    private int attendancePercent;
    private List<Day> days;

    @Data
    @Builder
    public static class Day {
        private LocalDate date;
        private String dayLabel;
        private String displayStatus;
        private boolean applicable;
    }
}