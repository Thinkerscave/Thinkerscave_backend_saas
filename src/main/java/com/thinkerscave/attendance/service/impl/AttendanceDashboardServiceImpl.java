package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.attendance.dto.response.AttendanceDashboardResponse;
import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import com.thinkerscave.attendance.repository.StaffAttendanceRepository;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.attendance.service.AttendanceDashboardService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceDashboardServiceImpl implements AttendanceDashboardService {

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StaffAttendanceRepository staffAttendanceRepository;

    @Override
    public AttendanceDashboardResponse getDashboardStats(LocalDate date) {
        Long orgId = OrganizationContext.getOrganizationId();

        // Use class-wise summary to count totals
        // row layout: [0]=classId, [1]=className, [2]=sectionId, [3]=sectionName, [4]=total, [5]=presentCount
        List<Object[]> classSummaries = studentAttendanceRepository.getClassWiseSummaryForDate(orgId, date);
        long studentsTotal = classSummaries.stream()
                .mapToLong(row -> ((Number) row[4]).longValue())
                .sum();
        long studentsPresent = classSummaries.stream()
                .mapToLong(row -> ((Number) row[5]).longValue())
                .sum();
        long studentsAbsent = studentsTotal - studentsPresent;
        long studentsLate = 0L;  // period-level late stats require a separate query
        double overallPercent = studentsTotal > 0
                ? ((studentsPresent + studentsLate) * 100.0 / studentsTotal) : 0.0;

        // Staff stats
        long staffPresent = staffAttendanceRepository
                .countByOrganizationIdAndAttendanceDateAndStatus(orgId, date, StaffAttendanceStatus.PRESENT);
        long staffAbsent = staffAttendanceRepository
                .countByOrganizationIdAndAttendanceDateAndStatus(orgId, date, StaffAttendanceStatus.ABSENT);
        long staffLate = staffAttendanceRepository
                .countByOrganizationIdAndAttendanceDateAndStatus(orgId, date, StaffAttendanceStatus.LATE);
        long staffOnLeave = staffAttendanceRepository
                .countByOrganizationIdAndAttendanceDateAndStatus(orgId, date, StaffAttendanceStatus.ON_LEAVE);

        return AttendanceDashboardResponse.builder()
                .studentsPresent(studentsPresent)
                .studentsAbsent(studentsAbsent)
                .studentsLate(studentsLate)
                .pendingClassCount(0L)
                .studentOverallPercent(Math.round(overallPercent * 100.0) / 100.0)
                .staffPresent(staffPresent)
                .staffAbsent(staffAbsent)
                .staffLate(staffLate)
                .staffOnLeave(staffOnLeave)
                .pendingClasses(List.of())
                .build();
    }
}
