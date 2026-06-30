package com.thinkerscave.attendance.controller;

import com.thinkerscave.attendance.dto.request.AttendanceReportRequest;
import com.thinkerscave.attendance.dto.response.AttendanceSummaryReportResponse;
import com.thinkerscave.attendance.dto.response.StaffAttendanceSummaryResponse;
import com.thinkerscave.attendance.dto.response.StudentHistoryResponse;
import com.thinkerscave.attendance.service.AttendanceReportService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance/reports")
@Tag(name = "Attendance Reports", description = "Attendance summary, trend, and individual reports")
@RequiredArgsConstructor
@Slf4j
public class AttendanceReportController {

    private final AttendanceReportService attendanceReportService;

    @PostMapping("/summary")
    @Operation(summary = "Get class-wise attendance summary for a date range")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<AttendanceSummaryReportResponse>> getSummaryReport(
            @Valid @RequestBody AttendanceReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Summary report generated",
                attendanceReportService.getSummaryReport(request)));
    }

    @PostMapping("/staff")
    @Operation(summary = "Get staff attendance summary for a date range")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<List<StaffAttendanceSummaryResponse>>> getStaffReport(
            @Valid @RequestBody AttendanceReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Staff report generated",
                attendanceReportService.getStaffReport(request)));
    }

    @PostMapping("/student/{studentId}")
    @Operation(summary = "Get individual student attendance report for a date range")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<StudentHistoryResponse>> getStudentReport(
            @PathVariable Long studentId,
            @Valid @RequestBody AttendanceReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Student report generated",
                attendanceReportService.getStudentReport(studentId, request)));
    }
}
