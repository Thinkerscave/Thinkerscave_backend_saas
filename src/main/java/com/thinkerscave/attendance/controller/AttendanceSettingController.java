package com.thinkerscave.attendance.controller;

import com.thinkerscave.attendance.dto.request.AttendanceFreezeRequest;
import com.thinkerscave.attendance.dto.response.AttendanceFreezeResponse;
import com.thinkerscave.attendance.service.AttendanceFreezeService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance/settings")
@Tag(name = "Attendance Freeze Periods", description = "Manage organization attendance freeze periods")
@RequiredArgsConstructor
@Slf4j
public class AttendanceSettingController {

    private final AttendanceFreezeService attendanceFreezeService;

    // ─── Freeze ───────────────────────────────────────────────────────────────

    @GetMapping("/freeze")
    @Operation(summary = "Get all active attendance freeze periods")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<List<AttendanceFreezeResponse>>> getAllFreezes() {
        return ResponseEntity.ok(ApiResponse.success(
                "Freeze records retrieved",
                attendanceFreezeService.getAllFreezes()));
    }

    @PostMapping("/freeze")
    @Operation(summary = "Create an attendance freeze period")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<AttendanceFreezeResponse>> createFreeze(
            @Valid @RequestBody AttendanceFreezeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                "Freeze period created",
                attendanceFreezeService.createFreeze(request)));
    }

    @DeleteMapping("/freeze/{freezeId}")
    @Operation(summary = "Deactivate an attendance freeze period")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteFreeze(@PathVariable Long freezeId) {
        attendanceFreezeService.deleteFreeze(freezeId);
        return ResponseEntity.ok(ApiResponse.noContent("Freeze period deactivated"));
    }
}
