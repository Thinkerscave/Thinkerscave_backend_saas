package com.thinkerscave.attendance.controller;

import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.attendance.dto.AttendanceRosterRecord;
import com.thinkerscave.attendance.entity.StudentAttendance;
import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.attendance.service.AttendanceFreezeService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Compatibility controller that exposes student attendance via the flat roster format
 * used by the frontend workspace ({@code SchoolOperationsDataService}).
 *
 * <p>Base path: {@code /api/v1/attendance}
 *
 * <p>This controller co-exists with {@link StudentAttendanceController} at
 * {@code /api/v1/attendance/students} — no path conflicts exist.
 */
@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "Attendance Roster", description = "Flat roster attendance APIs used by the frontend workspace")
@RequiredArgsConstructor
@Slf4j
public class AttendanceRosterController {

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final AttendanceFreezeService attendanceFreezeService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET  /api/v1/attendance?date=&type=
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all attendance records for a date and type (CLASS or STAFF)")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<List<AttendanceRosterRecord>> getAttendanceByDateAndType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "CLASS") String type) {

        Long orgId = OrganizationContext.getOrganizationId();
        log.debug("Fetching {} attendance for orgId={} date={}", type, orgId, date);

        if (!"CLASS".equalsIgnoreCase(type)) {
            // STAFF and HOSTEL handled by dedicated controllers — return empty for now
            return ResponseEntity.ok(List.of());
        }

        List<StudentAttendance> records =
                studentAttendanceRepository.findByOrganizationIdAndAttendanceDateOrderByClassNameAscRollNumberAsc(orgId, date);

        return ResponseEntity.ok(records.stream().map(this::toRosterRecord).collect(Collectors.toList()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET  /api/v1/attendance/class/{classId}?date=
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/class/{classId}")
    @Operation(summary = "Get attendance records for a specific class on a date")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<List<AttendanceRosterRecord>> getAttendanceByClass(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long orgId = OrganizationContext.getOrganizationId();
        log.debug("Fetching class attendance for orgId={} classId={} date={}", orgId, classId, date);

        List<StudentAttendance> records =
                studentAttendanceRepository.findByOrganizationIdAndClassIdAndAttendanceDateOrderByRollNumber(orgId, classId, date);

        return ResponseEntity.ok(records.stream().map(this::toRosterRecord).collect(Collectors.toList()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/attendance  — upsert a single student attendance record
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Save or update a single student attendance record")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<AttendanceRosterRecord> saveAttendance(
            @RequestBody AttendanceRosterRecord payload) {

        Long orgId = OrganizationContext.getOrganizationId();
        LocalDate date = LocalDate.parse(payload.getAttendanceDate());
        validateNotFrozen(orgId, date);

        try {
            StudentAttendance attendance = studentAttendanceRepository
                    .findByOrganizationIdAndStudentIdAndAttendanceDate(orgId, payload.getReferenceId(), date)
                    .orElseGet(StudentAttendance::new);

            populateAttendance(attendance, payload, orgId, date);
            attendance = studentAttendanceRepository.save(attendance);

            log.debug("Saved attendance id={} student={} status={}", attendance.getAttendanceId(),
                    payload.getReferenceId(), payload.getStatus());

            return ResponseEntity.ok(toRosterRecord(attendance));

        } catch (DataIntegrityViolationException ex) {
            // Race condition: concurrent POST already committed the record — find and update
            log.debug("Concurrent insert conflict for student={} date={}, retrying as update",
                    payload.getReferenceId(), date);
            return studentAttendanceRepository
                    .findByOrganizationIdAndStudentIdAndAttendanceDate(orgId, payload.getReferenceId(), date)
                    .map(existing -> {
                        populateAttendance(existing, payload, orgId, date);
                        return ResponseEntity.ok(toRosterRecord(studentAttendanceRepository.save(existing)));
                    })
                    .orElseThrow(() -> ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT  /api/v1/attendance/{id}  — update an existing record
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing student attendance record")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<AttendanceRosterRecord> updateAttendance(
            @PathVariable Long id,
            @RequestBody AttendanceRosterRecord payload) {

        Long orgId = OrganizationContext.getOrganizationId();

        StudentAttendance attendance = studentAttendanceRepository
                .findByOrganizationIdAndAttendanceId(orgId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id));

        LocalDate date = payload.getAttendanceDate() != null
                ? LocalDate.parse(payload.getAttendanceDate())
                : attendance.getAttendanceDate();
        validateNotFrozen(orgId, date);

        populateAttendance(attendance, payload, orgId, date);
        attendance = studentAttendanceRepository.save(attendance);

        return ResponseEntity.ok(toRosterRecord(attendance));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void populateAttendance(StudentAttendance a, AttendanceRosterRecord p, Long orgId, LocalDate date) {
        a.setOrganizationId(orgId);
        a.setStudentId(p.getReferenceId());
        a.setStudentName(p.getReferenceName());
        a.setAttendanceDate(date);
        a.setStatus(parseStatus(p.getStatus()));
        a.setRemarks(p.getRemarks());
        a.setMarkedBy(currentUser());

        // Resolve class info — try payload first, then fall back to student enrollment
        Long classId = p.getClassId();
        String className = p.getClassName();

        if (classId == null && p.getReferenceId() != null) {
            var enrollment = studentEnrollmentRepository.findActiveWithClassByStudentId(p.getReferenceId());
            if (enrollment.isPresent()) {
                var cls = enrollment.get().getClassEntity();
                if (cls != null) {
                    classId = cls.getClassId();
                    className = cls.getClassName();
                    if (a.getAcademicYearId() == null && cls.getAcademicYear() != null) {
                        a.setAcademicYearId(cls.getAcademicYear().getAcademicYearId());
                    }
                }
                var sec = enrollment.get().getSection();
                if (sec != null) {
                    a.setSectionId(sec.getSectionId());
                    a.setSectionName(sec.getSectionName());
                }
            }
        }

        if (classId != null) {
            a.setClassId(classId);
        }
        if (className != null) {
            a.setClassName(className);
        }

        if (p.getSectionName() != null) {
            a.setSectionName(p.getSectionName());
            if (a.getSectionId() == null && classId != null) {
                sectionRepository.findByAcademicClass_ClassIdAndNameIgnoreCase(classId, p.getSectionName())
                        .ifPresent(sec -> a.setSectionId(sec.getSectionId()));
            }
        }

        // Resolve academicYearId — use current year if still not set
        if (a.getAcademicYearId() == null) {
            academicYearRepository.findByCurrentYearTrue()
                    .ifPresentOrElse(
                            year -> a.setAcademicYearId(year.getAcademicYearId()),
                            () -> academicYearRepository.findByActiveOrderByStartDateDesc(true)
                                    .stream().findFirst()
                                    .ifPresent(year -> a.setAcademicYearId(year.getAcademicYearId()))
                    );
        }
    }

    private StudentAttendanceStatus parseStatus(String raw) {
        if (raw == null) return StudentAttendanceStatus.ABSENT;
        try {
            return StudentAttendanceStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Map frontend aliases
            return switch (raw.toUpperCase()) {
                case "EXCUSED", "LEAVE" -> StudentAttendanceStatus.EXCUSED;
                case "HALF_DAY", "HALFDAY" -> StudentAttendanceStatus.HALF_DAY;
                case "LATE" -> StudentAttendanceStatus.LATE;
                case "PRESENT" -> StudentAttendanceStatus.PRESENT;
                default -> StudentAttendanceStatus.ABSENT;
            };
        }
    }

    private AttendanceRosterRecord toRosterRecord(StudentAttendance a) {
        return AttendanceRosterRecord.builder()
                .attendanceId(a.getAttendanceId())
                .attendanceType("CLASS")
                .referenceId(a.getStudentId())
                .referenceName(a.getStudentName())
                .attendanceDate(a.getAttendanceDate() != null ? a.getAttendanceDate().toString() : null)
                .status(a.getStatus() != null ? a.getStatus().name() : null)
                .classId(a.getClassId())
                .className(a.getClassName())
                .sectionName(a.getSectionName())
                .remarks(a.getRemarks())
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
