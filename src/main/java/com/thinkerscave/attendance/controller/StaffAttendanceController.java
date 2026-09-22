package com.thinkerscave.attendance.controller;

import com.thinkerscave.attendance.dto.request.MarkStaffAttendanceRequest;
import com.thinkerscave.attendance.dto.request.StaffSignInRequest;
import com.thinkerscave.attendance.dto.request.StaffSignOutRequest;
import com.thinkerscave.attendance.dto.response.StaffAttendanceHistoryDayResponse;
import com.thinkerscave.attendance.dto.response.StaffAttendanceResponse;
import com.thinkerscave.attendance.dto.response.StaffAttendanceTodayResponse;
import com.thinkerscave.attendance.security.AttendanceAccessGuard;
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
    private final AttendanceAccessGuard accessGuard;

    @PostMapping("/mark")
    @Operation(summary = "Mark attendance for a staff member (manage privilege)")
    public ResponseEntity<ApiResponse<StaffAttendanceResponse>> markAttendance(
            @Valid @RequestBody MarkStaffAttendanceRequest request) {
        accessGuard.requireManage(AttendanceAccessGuard.ATTENDANCE_STAFF);
        log.info("Marking staff attendance for staff {}, date {}", request.getStaffId(), request.getAttendanceDate());
        return ResponseEntity.ok(ApiResponse.success(
                "Staff attendance marked",
                staffAttendanceService.markAttendance(request)));
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Record staff sign-in time using the server clock")
    public ResponseEntity<ApiResponse<StaffAttendanceTodayResponse>> signIn(
            @RequestBody(required = false) StaffSignInRequest request) {
        accessGuard.requireView(AttendanceAccessGuard.ATTENDANCE_STAFF);
        return ResponseEntity.ok(ApiResponse.success(
                "Sign-in recorded",
                staffAttendanceService.signIn(request != null ? request : new StaffSignInRequest())));
    }

    @PostMapping("/sign-out")
    @Operation(summary = "Record staff sign-out time using the server clock")
    public ResponseEntity<ApiResponse<StaffAttendanceTodayResponse>> signOut(
            @RequestBody(required = false) StaffSignOutRequest request) {
        accessGuard.requireView(AttendanceAccessGuard.ATTENDANCE_STAFF);
        return ResponseEntity.ok(ApiResponse.success(
                "Sign-out recorded",
                staffAttendanceService.signOut(request != null ? request : new StaffSignOutRequest())));
    }

    @GetMapping("/me/today")
    @Operation(summary = "Get the current staff member's authoritative attendance widget state for today")
    public ResponseEntity<ApiResponse<StaffAttendanceTodayResponse>> getMyTodayStatus(Authentication authentication) {
        accessGuard.requireView(AttendanceAccessGuard.ATTENDANCE_STAFF);
        return ResponseEntity.ok(ApiResponse.success(
                "Today's attendance status retrieved",
                staffAttendanceService.getMyTodayStatus(authentication.getName())));
    }

    @GetMapping("/me/history")
    @Operation(summary = "Get the authenticated staff member's attendance history for a date range (no pagination)")
    public ResponseEntity<ApiResponse<List<StaffAttendanceHistoryDayResponse>>> getMyHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        accessGuard.requireView(AttendanceAccessGuard.ATTENDANCE_STAFF);
        return ResponseEntity.ok(ApiResponse.success(
                "Attendance history retrieved",
                staffAttendanceService.getMyHistory(from, to)));
    }

    @GetMapping("/today")
    @Operation(summary = "Get all staff attendance for today or a specific date")
    public ResponseEntity<ApiResponse<List<StaffAttendanceResponse>>> getTodayAttendance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        accessGuard.requireView(AttendanceAccessGuard.ATTENDANCE_STAFF);
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                "Staff attendance retrieved",
                staffAttendanceService.getTodayAttendance(effectiveDate)));
    }

    @GetMapping("/history/{staffId}")
    @Operation(summary = "Get paginated attendance history for a staff member")
    public ResponseEntity<ApiResponse<Page<StaffAttendanceResponse>>> getStaffHistory(
            @PathVariable Long staffId,
            @PageableDefault(size = 10, sort = "attendanceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        accessGuard.requireView(AttendanceAccessGuard.ATTENDANCE_STAFF);
        return ResponseEntity.ok(ApiResponse.success(
                "Staff history retrieved",
                staffAttendanceService.getStaffHistory(staffId, pageable)));
    }

    @GetMapping("/history/{staffId}/range")
    @Operation(summary = "Get staff attendance within a date range")
    public ResponseEntity<ApiResponse<List<StaffAttendanceResponse>>> getStaffAttendanceByRange(
            @PathVariable Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        accessGuard.requireView(AttendanceAccessGuard.ATTENDANCE_STAFF);
        return ResponseEntity.ok(ApiResponse.success(
                "Staff attendance retrieved",
                staffAttendanceService.getStaffAttendanceByRange(staffId, from, to)));
    }
}
