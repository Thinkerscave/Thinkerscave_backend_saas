package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.attendance.dto.request.MarkStudentAttendanceRequest;
import com.thinkerscave.attendance.dto.request.MarkPeriodAttendanceRequest;
import com.thinkerscave.attendance.dto.request.StudentAttendanceEntry;
import com.thinkerscave.attendance.dto.request.UpdateStudentAttendanceRequest;
import com.thinkerscave.attendance.dto.response.ClassAttendanceSummaryResponse;
import com.thinkerscave.attendance.dto.response.StudentAttendanceResponse;
import com.thinkerscave.attendance.dto.response.StudentHistoryResponse;
import com.thinkerscave.attendance.entity.StudentAttendance;
import com.thinkerscave.attendance.entity.StudentPeriodAttendance;
import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.attendance.repository.StudentPeriodAttendanceRepository;
import com.thinkerscave.attendance.service.AttendanceFreezeService;
import com.thinkerscave.attendance.service.StudentAttendanceService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentAttendanceServiceImpl implements StudentAttendanceService {

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StudentPeriodAttendanceRepository studentPeriodAttendanceRepository;
    private final AttendanceFreezeService attendanceFreezeService;

    @Override
    @Transactional
    public ClassAttendanceSummaryResponse markDailyAttendance(MarkStudentAttendanceRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        validateNotFrozen(orgId, request.getAttendanceDate());

        String markedBy = currentUser();
        List<StudentAttendance> saved = request.getEntries().stream()
                .map(entry -> upsertDailyAttendance(orgId, request, entry, markedBy))
                .collect(Collectors.toList());

        return buildClassSummary(saved, request.getAttendanceDate(),
                request.getClassId(), request.getSectionId());
    }

    @Override
    @Transactional
    public ClassAttendanceSummaryResponse markPeriodAttendance(MarkPeriodAttendanceRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        validateNotFrozen(orgId, request.getAttendanceDate());

        String markedBy = currentUser();
        request.getEntries().forEach(entry ->
                upsertPeriodAttendance(orgId, request, entry, markedBy));

        List<StudentAttendance> classDayList = studentAttendanceRepository
                .findByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateOrderByRollNumber(
                        orgId, request.getClassId(), request.getSectionId(), request.getAttendanceDate());

        return buildClassSummary(classDayList, request.getAttendanceDate(),
                request.getClassId(), request.getSectionId());
    }

    @Override
    @Transactional
    public ClassAttendanceSummaryResponse copyFromPreviousDay(Long classId, Long sectionId, LocalDate targetDate) {
        Long orgId = OrganizationContext.getOrganizationId();
        validateNotFrozen(orgId, targetDate);

        LocalDate previousDay = targetDate.minusDays(1);
        List<StudentAttendance> previous = studentAttendanceRepository
                .findByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateOrderByRollNumber(orgId, classId, sectionId, previousDay);

        if (previous.isEmpty()) {
            throw new BadRequestException("No attendance found for previous day: " + previousDay);
        }

        String markedBy = currentUser();
        List<StudentAttendance> copied = previous.stream()
                .map(prev -> {
                    StudentAttendance attendance = studentAttendanceRepository
                            .findByOrganizationIdAndStudentIdAndAttendanceDate(orgId, prev.getStudentId(), targetDate)
                            .orElseGet(StudentAttendance::new);

                    attendance.setOrganizationId(orgId);
                    attendance.setStudentId(prev.getStudentId());
                    attendance.setStudentName(prev.getStudentName());
                    attendance.setRollNumber(prev.getRollNumber());
                    attendance.setAdmissionNumber(prev.getAdmissionNumber());
                    attendance.setAcademicYearId(prev.getAcademicYearId());
                    attendance.setClassId(prev.getClassId());
                    attendance.setClassName(prev.getClassName());
                    attendance.setSectionId(prev.getSectionId());
                    attendance.setSectionName(prev.getSectionName());
                    attendance.setAttendanceDate(targetDate);
                    attendance.setStatus(prev.getStatus());
                    attendance.setRemarks("Copied from " + previousDay);
                    attendance.setMarkedBy(markedBy);
                    return studentAttendanceRepository.save(attendance);
                })
                .collect(Collectors.toList());

        return buildClassSummary(copied, targetDate, classId, sectionId);
    }

    @Override
    public ClassAttendanceSummaryResponse getClassAttendance(Long classId, Long sectionId, LocalDate date) {
        Long orgId = OrganizationContext.getOrganizationId();
        List<StudentAttendance> records = studentAttendanceRepository
                .findByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateOrderByRollNumber(orgId, classId, sectionId, date);
        return buildClassSummary(records, date, classId, sectionId);
    }

    @Override
    @Transactional
    public StudentAttendanceResponse updateAttendance(Long attendanceId, UpdateStudentAttendanceRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        StudentAttendance attendance = studentAttendanceRepository.findById(attendanceId)
                .filter(a -> a.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + attendanceId));

        validateNotFrozen(orgId, attendance.getAttendanceDate());

        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());
        attendance.setMarkedBy(currentUser());

        return toResponse(studentAttendanceRepository.save(attendance));
    }

    @Override
    public Page<StudentAttendanceResponse> getStudentHistory(Long studentId, Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        return studentAttendanceRepository
                .findByOrganizationIdAndStudentIdOrderByAttendanceDateDesc(orgId, studentId, pageable)
                .map(this::toResponse);
    }

    @Override
    public StudentHistoryResponse getStudentHistoryByRange(Long studentId, LocalDate from, LocalDate to) {
        Long orgId = OrganizationContext.getOrganizationId();
        List<StudentAttendance> records = studentAttendanceRepository
                .findByOrganizationIdAndStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        orgId, studentId, from, to);

        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No attendance records found for student: " + studentId);
        }

        StudentAttendance first = records.get(0);
        int total = records.size();
        int present = (int) records.stream().filter(r ->
                r.getStatus() == StudentAttendanceStatus.PRESENT || r.getStatus() == StudentAttendanceStatus.LATE).count();
        int absent = (int) records.stream().filter(r -> r.getStatus() == StudentAttendanceStatus.ABSENT).count();
        int late = (int) records.stream().filter(r -> r.getStatus() == StudentAttendanceStatus.LATE).count();
        int excused = (int) records.stream().filter(r -> r.getStatus() == StudentAttendanceStatus.EXCUSED).count();

        List<StudentHistoryResponse.DayRecord> dayRecords = records.stream()
                .map(r -> StudentHistoryResponse.DayRecord.builder()
                        .date(r.getAttendanceDate())
                        .status(r.getStatus())
                        .remarks(r.getRemarks())
                        .build())
                .collect(Collectors.toList());

        double percent = total > 0 ? (present * 100.0 / total) : 0.0;

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

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private StudentAttendance upsertDailyAttendance(Long orgId, MarkStudentAttendanceRequest request,
            StudentAttendanceEntry entry, String markedBy) {

        StudentAttendance attendance = studentAttendanceRepository
                .findByOrganizationIdAndStudentIdAndAttendanceDate(orgId, entry.getStudentId(),
                        request.getAttendanceDate())
                .orElseGet(StudentAttendance::new);

        attendance.setOrganizationId(orgId);
        attendance.setStudentId(entry.getStudentId());
        attendance.setStudentName(entry.getStudentName());
        attendance.setRollNumber(entry.getRollNumber());
        attendance.setAdmissionNumber(entry.getAdmissionNumber());
        attendance.setAcademicYearId(request.getAcademicYearId());
        attendance.setClassId(request.getClassId());
        attendance.setSectionId(request.getSectionId());
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(entry.getStatus());
        attendance.setRemarks(entry.getRemarks());
        attendance.setMarkedBy(markedBy);

        return studentAttendanceRepository.save(attendance);
    }

    private void upsertPeriodAttendance(Long orgId, MarkPeriodAttendanceRequest request,
            StudentAttendanceEntry entry, String markedBy) {

        StudentPeriodAttendance attendance = studentPeriodAttendanceRepository
                .findByOrganizationIdAndStudentIdAndAttendanceDateAndPeriodId(
                        orgId, entry.getStudentId(), request.getAttendanceDate(), request.getPeriodId())
                .orElseGet(StudentPeriodAttendance::new);

        attendance.setOrganizationId(orgId);
        attendance.setStudentId(entry.getStudentId());
        attendance.setStudentName(entry.getStudentName());
        attendance.setRollNumber(entry.getRollNumber());
        attendance.setAcademicYearId(request.getAcademicYearId());
        attendance.setClassId(request.getClassId());
        attendance.setSectionId(request.getSectionId());
        attendance.setPeriodId(request.getPeriodId());
        attendance.setPeriodNumber(request.getPeriodNumber());
        attendance.setPeriodName(request.getPeriodName());
        attendance.setSubjectId(request.getSubjectId());
        attendance.setSubjectName(request.getSubjectName());
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(entry.getStatus());
        attendance.setRemarks(entry.getRemarks());
        attendance.setMarkedBy(markedBy);

        studentPeriodAttendanceRepository.save(attendance);
    }

    private ClassAttendanceSummaryResponse buildClassSummary(List<StudentAttendance> records,
            LocalDate date, Long classId, Long sectionId) {

        int total = records.size();
        int present = (int) records.stream()
                .filter(r -> r.getStatus() == StudentAttendanceStatus.PRESENT).count();
        int absent = (int) records.stream()
                .filter(r -> r.getStatus() == StudentAttendanceStatus.ABSENT).count();
        int late = (int) records.stream()
                .filter(r -> r.getStatus() == StudentAttendanceStatus.LATE).count();
        int excused = (int) records.stream()
                .filter(r -> r.getStatus() == StudentAttendanceStatus.EXCUSED).count();
        double percent = total > 0 ? ((present + late) * 100.0 / total) : 0.0;

        String className = records.isEmpty() ? null : records.get(0).getClassName();
        String sectionName = records.isEmpty() ? null : records.get(0).getSectionName();

        return ClassAttendanceSummaryResponse.builder()
                .attendanceDate(date)
                .classId(classId)
                .className(className)
                .sectionId(sectionId)
                .sectionName(sectionName)
                .totalStudents(total)
                .presentCount(present)
                .absentCount(absent)
                .lateCount(late)
                .excusedCount(excused)
                .attendancePercent(Math.round(percent * 100.0) / 100.0)
                .students(records.stream().map(this::toResponse).collect(Collectors.toList()))
                .build();
    }

    private StudentAttendanceResponse toResponse(StudentAttendance attendance) {
        return StudentAttendanceResponse.builder()
                .attendanceId(attendance.getAttendanceId())
                .studentId(attendance.getStudentId())
                .studentName(attendance.getStudentName())
                .rollNumber(attendance.getRollNumber())
                .admissionNumber(attendance.getAdmissionNumber())
                .classId(attendance.getClassId())
                .className(attendance.getClassName())
                .sectionId(attendance.getSectionId())
                .sectionName(attendance.getSectionName())
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .markedBy(attendance.getMarkedBy())
                .build();
    }

    private void validateNotFrozen(Long orgId, LocalDate date) {
        if (attendanceFreezeService.isDateFrozen(orgId, date)) {
            throw new BadRequestException("Attendance is frozen for date: " + date);
        }
    }

    private String currentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
