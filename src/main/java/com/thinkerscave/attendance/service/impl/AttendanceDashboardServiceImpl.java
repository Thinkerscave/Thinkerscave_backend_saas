package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.attendance.dto.response.AttendanceDashboardResponse;
import com.thinkerscave.attendance.dto.response.AttendanceDashboardResponse.PendingClassInfo;
import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
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
import java.util.stream.Collectors;

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
        long studentsLate = studentAttendanceRepository
                .countByOrganizationIdAndAttendanceDateAndStatus(orgId, date, StudentAttendanceStatus.LATE);
        long studentsAbsent = studentAttendanceRepository
                .countByOrganizationIdAndAttendanceDateAndStatus(orgId, date, StudentAttendanceStatus.ABSENT);
        if (studentsAbsent == 0 && studentsTotal > studentsPresent + studentsLate) {
            studentsAbsent = studentsTotal - studentsPresent - studentsLate;
        }
        double overallPercent = studentsTotal > 0
                ? ((studentsPresent + studentsLate) * 100.0 / studentsTotal) : 0.0;

        List<PendingClassInfo> pendingClasses = studentAttendanceRepository
                .findClassesWithPendingAttendance(orgId, date)
                .stream()
                .map(row -> PendingClassInfo.builder()
                        .classId(((Number) row[0]).longValue())
                        .className((String) row[1])
                        .build())
                .collect(Collectors.toList());

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
                .pendingClassCount((long) pendingClasses.size())
                .studentOverallPercent(Math.round(overallPercent * 100.0) / 100.0)
                .staffPresent(staffPresent)
                .staffAbsent(staffAbsent)
                .staffLate(staffLate)
                .staffOnLeave(staffOnLeave)
                .pendingClasses(pendingClasses)
                .build();
    }
}
