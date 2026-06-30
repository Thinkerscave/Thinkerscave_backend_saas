package com.thinkerscave.attendance.controller;

import com.thinkerscave.attendance.dto.request.MarkStudentAttendanceRequest;
import com.thinkerscave.attendance.dto.request.MarkPeriodAttendanceRequest;
import com.thinkerscave.attendance.dto.request.UpdateStudentAttendanceRequest;
import com.thinkerscave.attendance.dto.response.ClassAttendanceSummaryResponse;
import com.thinkerscave.attendance.dto.response.StudentAttendanceResponse;
import com.thinkerscave.attendance.dto.response.StudentHistoryResponse;
import com.thinkerscave.attendance.service.StudentAttendanceService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendance/students")
@Tag(name = "Student Attendance", description = "APIs for marking and querying student attendance")
@RequiredArgsConstructor
@Slf4j
public class StudentAttendanceController {

    private final StudentAttendanceService studentAttendanceService;

    @PostMapping("/daily")
    @Operation(summary = "Mark daily attendance for a class/section")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<ClassAttendanceSummaryResponse>> markDailyAttendance(
            @Valid @RequestBody MarkStudentAttendanceRequest request) {
        log.info("Marking daily attendance for class {}, section {}, date {}",
                request.getClassId(), request.getSectionId(), request.getAttendanceDate());
        return ResponseEntity.ok(ApiResponse.success(
                "Attendance marked successfully",
                studentAttendanceService.markDailyAttendance(request)));
    }

    @PostMapping("/period")
    @Operation(summary = "Mark period-wise attendance for a class/section")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<ClassAttendanceSummaryResponse>> markPeriodAttendance(
            @Valid @RequestBody MarkPeriodAttendanceRequest request) {
        log.info("Marking period attendance for class {}, period {}, date {}",
                request.getClassId(), request.getPeriodId(), request.getAttendanceDate());
        return ResponseEntity.ok(ApiResponse.success(
                "Period attendance marked successfully",
                studentAttendanceService.markPeriodAttendance(request)));
    }

    @PostMapping("/copy-previous")
    @Operation(summary = "Copy attendance from previous working day")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<ClassAttendanceSummaryResponse>> copyFromPreviousDay(
            @RequestParam Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        return ResponseEntity.ok(ApiResponse.success(
                "Attendance copied from previous day",
                studentAttendanceService.copyFromPreviousDay(classId, sectionId, targetDate)));
    }

    @GetMapping("/class")
    @Operation(summary = "Get attendance for a class/section on a date")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<ClassAttendanceSummaryResponse>> getClassAttendance(
            @RequestParam Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                "Attendance retrieved",
                studentAttendanceService.getClassAttendance(classId, sectionId, date)));
    }

    @PutMapping("/{attendanceId}")
    @Operation(summary = "Update a single student attendance record")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<StudentAttendanceResponse>> updateAttendance(
            @PathVariable Long attendanceId,
            @Valid @RequestBody UpdateStudentAttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Attendance updated",
                studentAttendanceService.updateAttendance(attendanceId, request)));
    }

    @GetMapping("/history/{studentId}")
    @Operation(summary = "Get paginated attendance history for a student")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<Page<StudentAttendanceResponse>>> getStudentHistory(
            @PathVariable Long studentId,
            @PageableDefault(size = 20, sort = "attendanceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Student history retrieved",
                studentAttendanceService.getStudentHistory(studentId, pageable)));
    }

    @GetMapping("/history/{studentId}/range")
    @Operation(summary = "Get attendance history for a student within a date range")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<StudentHistoryResponse>> getStudentHistoryByRange(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Student history retrieved",
                studentAttendanceService.getStudentHistoryByRange(studentId, from, to)));
    }
}
