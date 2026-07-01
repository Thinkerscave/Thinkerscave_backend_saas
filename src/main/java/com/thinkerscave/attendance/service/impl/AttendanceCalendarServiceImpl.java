package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.attendance.dto.response.AttendanceCalendarResponse;
import com.thinkerscave.attendance.dto.response.AttendanceCalendarResponse.CalendarDayResponse;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.attendance.service.AttendanceCalendarService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceCalendarServiceImpl implements AttendanceCalendarService {

    private final StudentAttendanceRepository studentAttendanceRepository;

    @Override
    public AttendanceCalendarResponse getCalendarData(Long classId, Long sectionId, int year, int month) {
        Long orgId = OrganizationContext.getOrganizationId();

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();

        // Fetch daily summary for the org within the month
        List<Object[]> dailySummary = studentAttendanceRepository.getDailyAttendanceSummary(orgId, from, to);

        // row: [0]=attendanceDate, [1]=totalStudents, [2]=presentCount
        Map<LocalDate, double[]> dateMap = dailySummary.stream()
                .collect(Collectors.toMap(
                        row -> (LocalDate) row[0],
                        row -> new double[]{((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue()}
                ));

        List<CalendarDayResponse> days = new ArrayList<>();
        for (int d = 1; d <= yearMonth.lengthOfMonth(); d++) {
            LocalDate current = yearMonth.atDay(d);
            double[] counts = dateMap.get(current);
            if (counts == null) {
                days.add(CalendarDayResponse.builder()
                        .date(current)
                        .attendancePercent(null)
                        .status("NO_DATA")
                        .build());
            } else {
                double total = counts[0];
                double present = counts[1];
                double pct = total > 0 ? Math.round((present / total * 100.0) * 100.0) / 100.0 : 0.0;
                String status;
                if (pct >= 85) status = "GOOD";
                else if (pct >= 70) status = "AVERAGE";
                else status = "LOW";

                days.add(CalendarDayResponse.builder()
                        .date(current)
                        .attendancePercent(pct)
                        .status(status)
                        .build());
            }
        }

        return AttendanceCalendarResponse.builder()
                .year(year)
                .month(month)
                .classId(classId)
                .sectionId(sectionId)
                .days(days)
                .build();
    }
}
