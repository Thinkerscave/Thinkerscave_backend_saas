package com.thinkerscave.attendance.controller;

import com.thinkerscave.attendance.dto.request.MarkStaffAttendanceRequest;
import com.thinkerscave.attendance.dto.request.StaffSignInRequest;
import com.thinkerscave.attendance.dto.request.StaffSignOutRequest;
import com.thinkerscave.attendance.dto.response.StaffAttendanceResponse;
import com.thinkerscave.attendance.service.StaffAttendanceService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance/staff")
@Tag(name = "Staff Attendance", description = "APIs for recording and querying staff attendance")
@RequiredArgsConstructor
@Slf4j
public class StaffAttendanceController {

    private final StaffAttendanceService staffAttendanceService;

    @PostMapping("/mark")
    @Operation(summary = "Mark attendance for a staff member (admin)")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<StaffAttendanceResponse>> markAttendance(
            @Valid @RequestBody MarkStaffAttendanceRequest request) {
        log.info("Marking staff attendance for staff {}, date {}", request.getStaffId(), request.getAttendanceDate());
        return ResponseEntity.ok(ApiResponse.success(
                "Staff attendance marked",
                staffAttendanceService.markAttendance(request)));
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Record staff sign-in time")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<StaffAttendanceResponse>> signIn(
            @Valid @RequestBody StaffSignInRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sign-in recorded",
                staffAttendanceService.signIn(request)));
    }

    @PostMapping("/sign-out")
    @Operation(summary = "Record staff sign-out time")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<StaffAttendanceResponse>> signOut(
            @Valid @RequestBody StaffSignOutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sign-out recorded",
                staffAttendanceService.signOut(request)));
    }

    @GetMapping("/me/today")
    @Operation(summary = "Get the current staff member's own sign-in/out status for today")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<StaffAttendanceResponse>> getMyTodayStatus(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Today's attendance status retrieved",
                staffAttendanceService.getMyTodayStatus(authentication.getName())));
    }

    @GetMapping("/today")
    @Operation(summary = "Get all staff attendance for today or a specific date")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<StaffAttendanceResponse>>> getTodayAttendance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                "Staff attendance retrieved",
                staffAttendanceService.getTodayAttendance(effectiveDate)));
    }

    @GetMapping("/history/{staffId}")
    @Operation(summary = "Get paginated attendance history for a staff member")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<StaffAttendanceResponse>>> getStaffHistory(
            @PathVariable Long staffId,
            @PageableDefault(size = 20, sort = "attendanceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Staff history retrieved",
                staffAttendanceService.getStaffHistory(staffId, pageable)));
    }

    @GetMapping("/history/{staffId}/range")
    @Operation(summary = "Get staff attendance within a date range")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<StaffAttendanceResponse>>> getStaffAttendanceByRange(
            @PathVariable Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Staff attendance retrieved",
                staffAttendanceService.getStaffAttendanceByRange(staffId, from, to)));
    }
}
