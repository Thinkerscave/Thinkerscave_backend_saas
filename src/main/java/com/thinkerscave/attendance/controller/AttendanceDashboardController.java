package com.thinkerscave.attendance.controller;

import com.thinkerscave.attendance.dto.response.AttendanceDashboardResponse;
import com.thinkerscave.attendance.service.AttendanceDashboardService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendance/dashboard")
@Tag(name = "Attendance Dashboard", description = "Attendance overview and summary statistics")
@RequiredArgsConstructor
@Slf4j
public class AttendanceDashboardController {

    private final AttendanceDashboardService attendanceDashboardService;

    @GetMapping
    @Operation(summary = "Get attendance dashboard statistics for today or a specific date")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<AttendanceDashboardResponse>> getDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                "Dashboard data retrieved",
                attendanceDashboardService.getDashboardStats(effectiveDate)));
    }
}
