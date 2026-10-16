package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.attendance.dto.request.MarkStudentAttendanceRequest;
import com.thinkerscave.attendance.dto.request.MarkPeriodAttendanceRequest;
import com.thinkerscave.attendance.dto.request.StudentAttendanceEntry;
import com.thinkerscave.attendance.dto.request.UpdateStudentAttendanceRequest;
import com.thinkerscave.attendance.dto.response.ClassAttendanceSummaryResponse;
import com.thinkerscave.attendance.dto.response.StudentAttendanceResponse;
import com.thinkerscave.attendance.dto.response.StudentAttendanceSessionResponse;
import com.thinkerscave.attendance.dto.response.StudentHistoryResponse;
import com.thinkerscave.attendance.dto.response.StudentRosterResponse;
import com.thinkerscave.attendance.entity.StudentAttendance;
import com.thinkerscave.attendance.entity.StudentPeriodAttendance;
import com.thinkerscave.attendance.enums.AttendanceMode;
import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.attendance.repository.StudentPeriodAttendanceRepository;
import com.thinkerscave.attendance.service.AttendanceFreezeService;
import com.thinkerscave.attendance.service.StaffAttendanceDayContextService;
import com.thinkerscave.attendance.service.StudentAttendanceService;
import com.thinkerscave.settings.ModuleSettingCatalog;
import com.thinkerscave.settings.ModuleSettingsService;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentAttendanceServiceImpl implements StudentAttendanceService {

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StudentPeriodAttendanceRepository studentPeriodAttendanceRepository;
    private final AttendanceFreezeService attendanceFreezeService;
        private final ModuleSettingsService moduleSettingsService;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StaffAttendanceDayContextService dayContextService;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;

    @Override
    public StudentRosterResponse getRoster(Long classId, Long sectionId, LocalDate date) {
        Long orgId = OrganizationContext.getOrganizationId();

        List<StudentEnrollment> enrollments = sectionId != null
                ? studentEnrollmentRepository.findActiveRosterByClassSection(classId, sectionId)
                : studentEnrollmentRepository.findActiveRosterByClass(classId);

        List<StudentAttendance> saved = studentAttendanceRepository
                .findByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateOrderByRollNumber(
                        orgId, classId, sectionId, date);
        Map<Long, StudentAttendance> savedByStudent = saved.stream()
                .collect(Collectors.toMap(StudentAttendance::getStudentId, Function.identity()));

        List<StudentRosterResponse.StudentRosterEntryResponse> students = enrollments.stream()
                .map(e -> toRosterEntry(e, savedByStudent.get(e.getStudent().getStudentId())))
                .collect(Collectors.toList());

        AcademicClass cls = classRepository.findByIdWithYear(classId).orElse(null);
        AcademicSection sec = sectionId != null
                ? sectionRepository.findById(sectionId).orElse(null)
                : null;

        String className = cls != null ? cls.getName() : (saved.isEmpty() ? null : saved.get(0).getClassName());
        String sectionName = sec != null
                ? sec.getName()
                : (saved.isEmpty() ? null : saved.get(0).getSectionName());

        Long academicYearId = cls != null && cls.getAcademicYear() != null
                ? cls.getAcademicYear().getAcademicYearId()
                : null;

        StaffAttendanceDayContextService.DayContext day = dayContextService.resolveStudentDay(orgId, date);
        AttendanceMode mode = AttendanceMode.valueOf(moduleSettingsService.getString(
                orgId, ModuleSettingCatalog.ATTENDANCE, ModuleSettingCatalog.ATTENDANCE_MODE));
        boolean frozen = attendanceFreezeService.isDateFrozen(orgId, date);

        return StudentRosterResponse.builder()
                .attendanceDate(date)
                .classId(classId)
                .className(className)
                .sectionId(sectionId)
                .sectionName(sectionName)
                .academicYearId(academicYearId)
                .attendanceMode(mode)
                .attendanceRequired(day.attendanceRequired())
                .holiday(day.holiday())
                .weekend(day.weekend())
                .holidayName(day.holidayName())
                .dayReason(day.reason())
                .frozen(frozen)
                .students(students)
                .build();
    }

    private StudentRosterResponse.StudentRosterEntryResponse toRosterEntry(
            StudentEnrollment e, StudentAttendance saved) {
        Student student = e.getStudent();
        String roll = e.getRollNumber() != null ? e.getRollNumber()
                : (student.getRollNumber() != null ? student.getRollNumber() : null);
        String name = studentName(student);
        return StudentRosterResponse.StudentRosterEntryResponse.builder()
                .studentId(student.getStudentId())
                .studentName(name)
                .rollNumber(roll)
                .admissionNumber(student.getAdmissionNumber())
                .status(saved != null ? saved.getStatus() : null)
                .remarks(saved != null ? saved.getRemarks() : null)
                .build();
    }

    @Override
    public List<StudentAttendanceSessionResponse> getSessions(
            LocalDate from, LocalDate to, Long classId, Long sectionId) {
        Long orgId = OrganizationContext.getOrganizationId();

        List<Object[]> rows = studentAttendanceRepository.getSessionList(orgId, from, to, classId, sectionId);
        List<StudentAttendanceSessionResponse> sessions = new ArrayList<>();

        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            Long cid = ((Number) row[1]).longValue();
            String cname = (String) row[2];
            Long sid = row[3] != null ? ((Number) row[3]).longValue() : null;
            String sname = (String) row[4];
            int totalMarked = ((Number) row[5]).intValue();
            int present = ((Number) row[6]).intValue();
            int absent = ((Number) row[7]).intValue();
            int late = ((Number) row[8]).intValue();
            int excused = ((Number) row[9]).intValue();

            long expected = sid != null
                    ? studentEnrollmentRepository.countByClassEntityClassIdAndSectionSectionIdAndActiveTrue(cid, sid)
                    : studentEnrollmentRepository.countByClassEntityClassIdAndActiveTrue(cid);

            StaffAttendanceDayContextService.DayContext day = dayContextService.resolveStudentDay(orgId, date);
            boolean complete = expected > 0 && totalMarked >= expected;
            double pct = totalMarked > 0
                    ? Math.round(((present + late) * 100.0 / totalMarked) * 100.0) / 100.0
                    : 0.0;

            sessions.add(StudentAttendanceSessionResponse.builder()
                    .date(date)
                    .classId(cid)
                    .className(cname)
                    .sectionId(sid)
                    .sectionName(sname)
                    .expectedStudents((int) expected)
                    .totalMarked(totalMarked)
                    .present(present)
                    .absent(absent)
                    .late(late)
                    .excused(excused)
                    .attendancePercent(pct)
                    .holiday(day.holiday())
                    .weekend(day.weekend())
                    .dayReason(day.reason())
                    .status(complete ? "COMPLETED" : "PARTIAL")
                    .build());
        }
        return sessions;
    }

    @Override
    @Transactional
    public ClassAttendanceSummaryResponse markDailyAttendance(MarkStudentAttendanceRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        validateNotFrozen(orgId, request.getAttendanceDate());
        validateInstructionalDay(orgId, request.getAttendanceDate());

        ResolvedClassInfo resolved = resolveClassInfo(orgId, request.getClassId(), request.getSectionId());
        Set<Long> roster = activeRosterStudentIds(resolved.classId, resolved.sectionId);
        validateEntriesAgainstRoster(request.getEntries(), roster);

        String markedBy = currentUser();
        List<StudentAttendance> saved = request.getEntries().stream()
                .map(entry -> upsertDailyAttendance(orgId, request, entry, markedBy, resolved))
                .collect(Collectors.toList());

        return buildClassSummary(saved, request.getAttendanceDate(),
                resolved.classId, resolved.sectionId);
    }

    @Override
    @Transactional
    public ClassAttendanceSummaryResponse markPeriodAttendance(MarkPeriodAttendanceRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        validateNotFrozen(orgId, request.getAttendanceDate());
        validateInstructionalDay(orgId, request.getAttendanceDate());

        ResolvedClassInfo resolved = resolveClassInfo(orgId, request.getClassId(), request.getSectionId());
        Set<Long> roster = activeRosterStudentIds(resolved.classId, resolved.sectionId);
        validateEntriesAgainstRoster(request.getEntries(), roster);

        String markedBy = currentUser();
        request.getEntries().forEach(entry ->
                upsertPeriodAttendance(orgId, request, entry, markedBy, resolved));

        List<StudentAttendance> classDayList = studentAttendanceRepository
                .findByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateOrderByRollNumber(
                        orgId, resolved.classId, resolved.sectionId, request.getAttendanceDate());

        return buildClassSummary(classDayList, request.getAttendanceDate(),
                resolved.classId, resolved.sectionId);
    }

    @Override
    @Transactional
    public ClassAttendanceSummaryResponse copyFromPreviousDay(Long classId, Long sectionId, LocalDate targetDate) {
        Long orgId = OrganizationContext.getOrganizationId();
        validateNotFrozen(orgId, targetDate);
        validateInstructionalDay(orgId, targetDate);

        LocalDate previousDay = targetDate.minusDays(1);
        List<StudentAttendance> previous = sectionId != null
                ? studentAttendanceRepository
                        .findByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateOrderByRollNumber(
                                orgId, classId, sectionId, previousDay)
                : studentAttendanceRepository
                        .findByOrganizationIdAndClassIdAndAttendanceDateOrderByRollNumber(
                                orgId, classId, previousDay);

        if (previous.isEmpty()) {
            throw new BadRequestException("No attendance found for previous day: " + previousDay);
        }

        // Honor org setting — copy is a no-op when disabled
        boolean allowCopy = Boolean.TRUE.equals(moduleSettingsService.getBoolean(
                orgId, ModuleSettingCatalog.ATTENDANCE, ModuleSettingCatalog.ALLOW_COPY_PREVIOUS));
        if (!allowCopy) {
            throw new BadRequestException("Copy from previous day is disabled in attendance settings");
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
            StudentAttendanceEntry entry, String markedBy, ResolvedClassInfo resolved) {

        StudentAttendance attendance = studentAttendanceRepository
                .findByOrganizationIdAndStudentIdAndAttendanceDate(orgId, entry.getStudentId(),
                        request.getAttendanceDate())
                .orElseGet(StudentAttendance::new);

        attendance.setOrganizationId(orgId);
        attendance.setStudentId(entry.getStudentId());
        attendance.setStudentName(entry.getStudentName());
        attendance.setRollNumber(entry.getRollNumber());
        attendance.setAdmissionNumber(entry.getAdmissionNumber());
        attendance.setAcademicYearId(resolved.academicYearId);
        attendance.setClassId(resolved.classId);
        attendance.setClassName(resolved.className);
        attendance.setSectionId(resolved.sectionId);
        attendance.setSectionName(resolved.sectionName);
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(entry.getStatus());
        attendance.setRemarks(entry.getRemarks());
        attendance.setMarkedBy(markedBy);

        return studentAttendanceRepository.save(attendance);
    }

    private void upsertPeriodAttendance(Long orgId, MarkPeriodAttendanceRequest request,
            StudentAttendanceEntry entry, String markedBy, ResolvedClassInfo resolved) {

        StudentPeriodAttendance attendance = studentPeriodAttendanceRepository
                .findByOrganizationIdAndStudentIdAndAttendanceDateAndPeriodId(
                        orgId, entry.getStudentId(), request.getAttendanceDate(), request.getPeriodId())
                .orElseGet(StudentPeriodAttendance::new);

        attendance.setOrganizationId(orgId);
        attendance.setStudentId(entry.getStudentId());
        attendance.setStudentName(entry.getStudentName());
        attendance.setRollNumber(entry.getRollNumber());
        attendance.setAcademicYearId(resolved.academicYearId);
        attendance.setClassId(resolved.classId);
        attendance.setClassName(resolved.className);
        attendance.setSectionId(resolved.sectionId);
        attendance.setSectionName(resolved.sectionName);
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

    private void validateInstructionalDay(Long orgId, LocalDate date) {
        StaffAttendanceDayContextService.DayContext day = dayContextService.resolveStudentDay(orgId, date);
        if (!day.attendanceRequired()) {
            String reason = day.holiday() ? "holiday" : "weekend";
            String detail = day.holiday() && day.holidayName() != null ? " (" + day.holidayName() + ")" : "";
            throw new BadRequestException(
                    "Attendance cannot be marked on a " + reason + detail + ": " + date);
        }
    }

    private ResolvedClassInfo resolveClassInfo(Long orgId, Long classId, Long sectionId) {
        AcademicClass cls = classRepository.findByIdWithYear(classId)
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .orElseThrow(() -> new BadRequestException("Class not found or inactive: " + classId));
        String className = cls.getName();

        Long resolvedSectionId = sectionId;
        String resolvedSectionName = null;
        if (sectionId != null) {
            AcademicSection sec = sectionRepository.findById(sectionId)
                    .filter(s -> Boolean.TRUE.equals(s.getActive()))
                    .orElseThrow(() -> new BadRequestException("Section not found or inactive: " + sectionId));
            resolvedSectionName = sec.getName();
        }

        Long academicYearId = cls.getAcademicYear() != null ? cls.getAcademicYear().getAcademicYearId() : null;

        return new ResolvedClassInfo(classId, className, resolvedSectionId, resolvedSectionName, academicYearId);
    }

    private Set<Long> activeRosterStudentIds(Long classId, Long sectionId) {
        List<StudentEnrollment> enrollments = sectionId != null
                ? studentEnrollmentRepository.findActiveRosterByClassSection(classId, sectionId)
                : studentEnrollmentRepository.findActiveRosterByClass(classId);
        return enrollments.stream()
                .map(e -> e.getStudent().getStudentId())
                .collect(Collectors.toSet());
    }

    private void validateEntriesAgainstRoster(List<StudentAttendanceEntry> entries, Set<Long> roster) {
        if (entries.isEmpty()) {
            throw new BadRequestException("At least one student entry is required");
        }
        Set<Long> seen = new HashSet<>();
        for (StudentAttendanceEntry entry : entries) {
            if (entry.getStudentId() == null) {
                throw new BadRequestException("Student ID is required for every entry");
            }
            if (!seen.add(entry.getStudentId())) {
                throw new BadRequestException("Duplicate entry for student: " + entry.getStudentId());
            }
            if (!roster.contains(entry.getStudentId())) {
                throw new BadRequestException(
                        "Student is not enrolled in this class/section: " + entry.getStudentId());
            }
        }
    }

    private String studentName(Student student) {
        return java.util.stream.Stream.of(student.getFirstName(), student.getMiddleName(), student.getLastName())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "))
                .trim();
    }

    private record ResolvedClassInfo(Long classId, String className, Long sectionId,
            String sectionName, Long academicYearId) {
    }

    private String currentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
