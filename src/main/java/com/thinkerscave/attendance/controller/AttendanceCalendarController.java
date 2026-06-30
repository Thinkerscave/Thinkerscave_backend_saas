package com.thinkerscave.attendance.controller;

import com.thinkerscave.attendance.dto.response.AttendanceCalendarResponse;
import com.thinkerscave.attendance.service.AttendanceCalendarService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance/calendar")
@Tag(name = "Attendance Calendar", description = "Monthly attendance calendar view")
@RequiredArgsConstructor
@Slf4j
public class AttendanceCalendarController {

    private final AttendanceCalendarService attendanceCalendarService;

    @GetMapping
    @Operation(summary = "Get monthly calendar attendance data for a class/section")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<AttendanceCalendarResponse>> getCalendar(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(
                "Calendar data retrieved",
                attendanceCalendarService.getCalendarData(classId, sectionId, year, month)));
    }
}
