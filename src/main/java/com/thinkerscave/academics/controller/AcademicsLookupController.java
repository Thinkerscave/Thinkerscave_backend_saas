package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.response.LookupDTO;
import com.thinkerscave.academics.service.AcademicsLookupService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academics/lookup")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
@Tag(name = "Academics Lookup", description = "Lookup endpoints for dropdowns")
public class AcademicsLookupController {

    private final AcademicsLookupService lookupService;

    @GetMapping("/years")
    @Operation(summary = "Get active academic years")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getYears() {
        return ResponseEntity.ok(ApiResponse.success("Academic years", lookupService.getActiveAcademicYears()));
    }

    @GetMapping("/years/{yearId}/academic-groups")
    @Operation(summary = "Get academicGroups for a year")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getAcademicGroups(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("AcademicGroups", lookupService.getAcademicGroupsByYear(yearId)));
    }

    @GetMapping("/academic-groups/{academicGroupId}/sections")
    @Operation(summary = "Get sections for an Academic Group")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getSections(@PathVariable Long academicGroupId) {
        return ResponseEntity.ok(ApiResponse.success("Sections", lookupService.getSectionsByAcademicGroup(academicGroupId)));
    }

    @GetMapping("/subjects")
    @Operation(summary = "Get active subjects")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getSubjects() {
        return ResponseEntity.ok(ApiResponse.success("Subjects", lookupService.getActiveSubjects()));
    }

    @GetMapping("/years/{yearId}/schedules")
    @Operation(summary = "Get schedules for a year")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getSchedules(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Schedules", lookupService.getSchedulesByYear(yearId)));
    }

    @GetMapping("/schedules/{scheduleId}/templates")
    @Operation(summary = "Get templates for a schedule")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getTemplates(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(ApiResponse.success("Templates", lookupService.getTemplatesBySchedule(scheduleId)));
    }
}
