package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.response.*;
import com.thinkerscave.academics.service.AcademicsMeService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academics/me")
@RequiredArgsConstructor
@Tag(name = "Academics Me", description = "Teacher and student personal academic APIs")
public class AcademicsMeController {

    private final AcademicsMeService meService;

    // ─── Teacher endpoints ────────────────────────────────────────────────

    @GetMapping("/academic-groups")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get teacher's assigned academicGroups and subjects")
    public ResponseEntity<ApiResponse<TeacherMyAcademicGroupsResponse>> myAcademicGroups(
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Teacher academicGroups retrieved", meService.getTeacherMyAcademicGroups(academicYearId)));
    }

    @GetMapping("/structure")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get read-only academic structure relevant to teacher's allocations")
    public ResponseEntity<ApiResponse<TeacherAcademicStructureResponse>> myStructure(
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Academic structure retrieved", meService.getTeacherStructure(academicYearId)));
    }

    // ─── Role-branching timetable ─────────────────────────────────────────

    @GetMapping("/timetable")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','STUDENT','PARENT')")
        @Operation(summary = "Get personal timetable (teacher view for staff, Academic Group view for students)")
    public ResponseEntity<ApiResponse<MyTimetableResponse>> myTimetable(
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Timetable retrieved", meService.getMyTimetable(academicYearId)));
    }

    // ─── Student endpoints ────────────────────────────────────────────────

    @GetMapping("/academics")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STUDENT','PARENT')")
        @Operation(summary = "Get student academic profile, Academic Group, subjects and section teacher")
    public ResponseEntity<ApiResponse<StudentMyAcademicsResponse>> myAcademics(
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Student academics retrieved", meService.getStudentMyAcademics(academicYearId)));
    }
}
