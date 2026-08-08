package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.attendance.dto.request.AttendanceReportRequest;
import com.thinkerscave.attendance.dto.response.AttendanceSummaryReportResponse;
import com.thinkerscave.attendance.dto.response.AttendanceSummaryReportResponse.ClassSummaryRow;
import com.thinkerscave.attendance.dto.response.AttendanceSummaryReportResponse.DefaulterRow;
import com.thinkerscave.attendance.dto.response.AttendanceSummaryReportResponse.MonthlyTrendRow;
import com.thinkerscave.attendance.dto.response.StaffAttendanceSummaryResponse;
import com.thinkerscave.attendance.dto.response.StudentHistoryResponse;
import com.thinkerscave.attendance.entity.StudentAttendance;
import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import com.thinkerscave.attendance.repository.AttendanceSettingRepository;
import com.thinkerscave.attendance.repository.StaffAttendanceRepository;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.attendance.service.AttendanceReportService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceReportServiceImpl implements AttendanceReportService {

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StaffAttendanceRepository staffAttendanceRepository;
    private final AttendanceSettingRepository attendanceSettingRepository;

    @Override
    public AttendanceSummaryReportResponse getSummaryReport(AttendanceReportRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();

        // Class-wise summary across the selected date range (not a single day)
        // row: [0]=classId, [1]=className, [2]=sectionId, [3]=sectionName, [4]=total, [5]=presentCount
        List<Object[]> classRows = studentAttendanceRepository.getClassWiseSummaryForRange(
                orgId, request.getFromDate(), request.getToDate());

        List<ClassSummaryRow> classWiseSummary = classRows.stream()
                .map(row -> ClassSummaryRow.builder()
                        .classId(((Number) row[0]).longValue())
                        .className((String) row[1])
                        .sectionId(row[2] != null ? ((Number) row[2]).longValue() : null)
                        .sectionName((String) row[3])
                        .totalStudents(((Number) row[4]).intValue())
                        .avgAttendancePercent(((Number) row[4]).intValue() > 0
                                ? Math.round(((Number) row[5]).doubleValue()
                                / ((Number) row[4]).doubleValue() * 10000.0) / 100.0
                                : 0.0)
                        .build())
                .collect(Collectors.toList());

        // Defaulter list — prefer request threshold, else org minStudentAttendancePercent, else 75
        double threshold = request.getDefaulterThreshold() != null
                ? request.getDefaulterThreshold()
                : attendanceSettingRepository.findByOrganizationId(orgId)
                        .map(s -> s.getMinStudentAttendancePercent() != null
                                ? s.getMinStudentAttendancePercent().doubleValue() : 75.0)
                        .orElse(75.0);
        List<Object[]> defaulterRows = studentAttendanceRepository.getDefaulterList(
                orgId, request.getFromDate(), request.getToDate(), threshold);

        List<DefaulterRow> defaulters = defaulterRows.stream()
                .map(row -> DefaulterRow.builder()
                        .studentId(((Number) row[0]).longValue())
                        .studentName((String) row[1])
                        .className((String) row[2])
                        .sectionName((String) row[3])
                        .totalDays(((Number) row[4]).intValue())
                        .presentDays(((Number) row[5]).intValue())
                        .attendancePercent(((Number) row[4]).intValue() > 0
                                ? Math.round(((Number) row[5]).doubleValue()
                                / ((Number) row[4]).doubleValue() * 10000.0) / 100.0
                                : 0.0)
                        .build())
                .collect(Collectors.toList());

        List<MonthlyTrendRow> monthlyTrend = buildMonthlyTrend(
                orgId, request.getFromDate(), request.getToDate());

        long totalStudents = classWiseSummary.stream()
                .mapToLong(ClassSummaryRow::getTotalStudents).sum();
        double overallPercent = classWiseSummary.isEmpty() ? 0.0
                : classWiseSummary.stream()
                .mapToDouble(ClassSummaryRow::getAvgAttendancePercent)
                .average().orElse(0.0);

        return AttendanceSummaryReportResponse.builder()
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .totalStudents(totalStudents)
                .overallPercent(Math.round(overallPercent * 100.0) / 100.0)
                .classWiseSummary(classWiseSummary)
                .monthlyTrend(monthlyTrend)
                .defaulters(defaulters)
                .build();
    }

    private List<MonthlyTrendRow> buildMonthlyTrend(Long orgId, java.time.LocalDate from, java.time.LocalDate to) {
        // row: [0]=year, [1]=month, [2]=status, [3]=count
        List<Object[]> rows = studentAttendanceRepository.getMonthlyStatusBreakdown(orgId, from, to);
        Map<String, MonthlyAccumulator> byMonth = new LinkedHashMap<>();

        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            String status = row[2] instanceof StudentAttendanceStatus
                    ? ((StudentAttendanceStatus) row[2]).name()
                    : String.valueOf(row[2]);
            long count = ((Number) row[3]).longValue();
            String key = year + "-" + month;
            MonthlyAccumulator acc = byMonth.computeIfAbsent(key, k -> new MonthlyAccumulator(year, month));
            acc.statusBreakdown.merge(status, count, Long::sum);
            acc.total += count;
            if (StudentAttendanceStatus.PRESENT.name().equals(status)
                    || StudentAttendanceStatus.LATE.name().equals(status)) {
                acc.presentLike += count;
            }
        }

        List<MonthlyTrendRow> trend = new ArrayList<>();
        for (MonthlyAccumulator acc : byMonth.values()) {
            double pct = acc.total > 0
                    ? Math.round((acc.presentLike * 100.0 / acc.total) * 100.0) / 100.0
                    : 0.0;
            trend.add(MonthlyTrendRow.builder()
                    .year(acc.year)
                    .month(acc.month)
                    .avgAttendancePercent(pct)
                    .statusBreakdown(acc.statusBreakdown)
                    .build());
        }
        return trend;
    }

    private static final class MonthlyAccumulator {
        final int year;
        final int month;
        long total;
        long presentLike;
        final Map<String, Long> statusBreakdown = new LinkedHashMap<>();

        MonthlyAccumulator(int year, int month) {
            this.year = year;
            this.month = month;
        }
    }

    @Override
    public List<StaffAttendanceSummaryResponse> getStaffReport(AttendanceReportRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();

        // row: [0]=staffId, [1]=staffName, [2]=department, [3]=totalDays, [4]=presentDays, [5]=absentDays, [6]=lateDays
        List<Object[]> rows = staffAttendanceRepository.getStaffAttendanceSummary(
                orgId, request.getFromDate(), request.getToDate());

        return rows.stream()
                .map(row -> {
                    int total = ((Number) row[3]).intValue();
                    int present = ((Number) row[4]).intValue();
                    int absent = ((Number) row[5]).intValue();
                    int late = ((Number) row[6]).intValue();
                    double pct = total > 0 ? Math.round((present * 100.0 / total) * 100.0) / 100.0 : 0.0;

                    return StaffAttendanceSummaryResponse.builder()
                            .staffId(((Number) row[0]).longValue())
                            .staffName((String) row[1])
                            .department((String) row[2])
                            .totalDays(total)
                            .presentDays(present)
                            .absentDays(absent)
                            .lateDays(late)
                            .onLeaveDays(0)
                            .attendancePercent(pct)
                            .avgWorkingHours(0.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public StudentHistoryResponse getStudentReport(Long studentId, AttendanceReportRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();

        List<StudentAttendance> records = studentAttendanceRepository
                .findByOrganizationIdAndStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        orgId, studentId, request.getFromDate(), request.getToDate());

        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No attendance records found for student: " + studentId);
        }

        StudentAttendance first = records.get(0);
        int total = records.size();
        int present = (int) records.stream()
                .filter(r -> r.getStatus() == StudentAttendanceStatus.PRESENT
                        || r.getStatus() == StudentAttendanceStatus.LATE)
                .count();
        int absent = (int) records.stream()
                .filter(r -> r.getStatus() == StudentAttendanceStatus.ABSENT).count();
        int late = (int) records.stream()
                .filter(r -> r.getStatus() == StudentAttendanceStatus.LATE).count();
        int excused = (int) records.stream()
                .filter(r -> r.getStatus() == StudentAttendanceStatus.EXCUSED).count();

        double percent = total > 0 ? Math.round((present * 100.0 / total) * 100.0) / 100.0 : 0.0;

        List<StudentHistoryResponse.DayRecord> dayRecords = records.stream()
                .map(r -> StudentHistoryResponse.DayRecord.builder()
                        .date(r.getAttendanceDate())
                        .status(r.getStatus())
                        .remarks(r.getRemarks())
                        .build())
                .collect(Collectors.toList());

        return StudentHistoryResponse.builder()
                .studentId(studentId)
                .studentName(first.getStudentName())
                .rollNumber(first.getRollNumber())
                .admissionNumber(first.getAdmissionNumber())
                .classId(first.getClassId())
                .className(first.getClassName())
                .sectionId(first.getSectionId())
                .sectionName(first.getSectionName())
                .totalDays(total)
                .presentDays(present)
                .absentDays(absent)
                .lateDays(late)
                .excusedDays(excused)
                .attendancePercent(percent)
                .records(dayRecords)
                .build();
    }
}
