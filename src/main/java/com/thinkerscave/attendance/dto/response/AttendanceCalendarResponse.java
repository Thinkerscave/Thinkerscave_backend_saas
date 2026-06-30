package com.thinkerscave.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@Schema(description = "Monthly attendance calendar for a class/organization")
public class AttendanceCalendarResponse {

    private int year;
    private int month;
    private Long classId;
    private String className;
    private Long sectionId;
    private List<CalendarDayResponse> days;

    @Data
    @Builder
    @Schema(description = "Attendance data for a single calendar day")
    public static class CalendarDayResponse {
        private LocalDate date;
        private Double attendancePercent;
        @Schema(description = "GOOD=>=85%, AVERAGE=>=70%, LOW=<70%, NO_DATA=not marked, HOLIDAY")
        private String status;
    }
}
