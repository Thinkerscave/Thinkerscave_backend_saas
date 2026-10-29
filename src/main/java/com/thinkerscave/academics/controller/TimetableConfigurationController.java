package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.FixedActivityRequest;
import com.thinkerscave.academics.dto.request.FixedActivityResolveRequest;
import com.thinkerscave.academics.dto.request.GroupScheduleAssignmentRequest;
import com.thinkerscave.academics.dto.request.SchedulePatternRequest;
import com.thinkerscave.academics.dto.response.FixedActivityOptionsResponse;
import com.thinkerscave.academics.dto.response.FixedActivityResponse;
import com.thinkerscave.academics.dto.response.FixedActivityScheduleResponse;
import com.thinkerscave.academics.dto.response.GroupAssignmentMatrixResponse;
import com.thinkerscave.academics.dto.response.GroupScheduleAssignmentResponse;
import com.thinkerscave.academics.dto.response.SchedulePatternResponse;
import com.thinkerscave.academics.service.TimetableConfigurationService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
@Tag(name = "Timetable Configuration", description = "Schedule patterns, group assignments and fixed activities")
public class TimetableConfigurationController {

    private final TimetableConfigurationService service;

    @GetMapping("/years/{yearId}/timetable/patterns")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "List reusable schedule patterns for an academic year")
    public ResponseEntity<ApiResponse<List<SchedulePatternResponse>>> patterns(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Schedule patterns", service.listPatterns(yearId)));
    }

    @GetMapping("/timetable/patterns/{patternId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<SchedulePatternResponse>> pattern(@PathVariable Long patternId) {
        return ResponseEntity.ok(ApiResponse.success("Schedule pattern", service.getPattern(patternId)));
    }

    @PostMapping("/years/{yearId}/timetable/patterns")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SchedulePatternResponse>> createPattern(
            @PathVariable Long yearId, @Valid @RequestBody SchedulePatternRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                "Schedule pattern created", service.createPattern(yearId, request)));
    }

    @PutMapping("/timetable/patterns/{patternId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SchedulePatternResponse>> updatePattern(
            @PathVariable Long patternId, @Valid @RequestBody SchedulePatternRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Schedule pattern updated",
                service.updatePattern(patternId, request)));
    }

    @PostMapping("/timetable/patterns/{patternId}/duplicate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SchedulePatternResponse>> duplicatePattern(
            @PathVariable Long patternId, @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                "Schedule pattern duplicated",
                service.duplicatePattern(patternId, body == null ? null : body.get("name"))));
    }

    @PostMapping("/timetable/patterns/{patternId}/activate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SchedulePatternResponse>> activatePattern(@PathVariable Long patternId) {
        return ResponseEntity.ok(ApiResponse.success("Schedule pattern activated",
                service.setPatternActive(patternId, true)));
    }

    @PostMapping("/timetable/patterns/{patternId}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SchedulePatternResponse>> deactivatePattern(@PathVariable Long patternId) {
        return ResponseEntity.ok(ApiResponse.success("Schedule pattern deactivated",
                service.setPatternActive(patternId, false)));
    }

    @GetMapping("/years/{yearId}/timetable/assignments")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<List<GroupScheduleAssignmentResponse>>> assignments(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Group schedule assignments",
                service.listAssignments(yearId)));
    }

    @GetMapping("/years/{yearId}/timetable/assignments/matrix")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<GroupAssignmentMatrixResponse>> assignmentMatrix(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Group assignment matrix",
                service.getAssignmentMatrix(yearId)));
    }

    @PostMapping("/years/{yearId}/timetable/assignments")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<List<GroupScheduleAssignmentResponse>>> assign(
            @PathVariable Long yearId, @Valid @RequestBody GroupScheduleAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                "Schedule pattern assigned", service.assignPattern(yearId, request)));
    }

    @PutMapping("/timetable/assignments/{assignmentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<GroupScheduleAssignmentResponse>> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody GroupScheduleAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Schedule assignment updated",
                service.updateAssignment(assignmentId, request)));
    }

    @DeleteMapping("/timetable/assignments/{assignmentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(@PathVariable Long assignmentId) {
        service.deleteAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Schedule assignment removed", null));
    }

    @GetMapping("/years/{yearId}/timetable/fixed-activities/options")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Activity subjects and eligible groups/sections for fixed activities")
    public ResponseEntity<ApiResponse<FixedActivityOptionsResponse>> fixedActivityOptions(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Fixed activity options",
                service.getFixedActivityOptions(yearId)));
    }

    @PostMapping("/years/{yearId}/timetable/fixed-activities/resolve")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Resolve schedule patterns, periods and teacher options for the selected units/days")
    public ResponseEntity<ApiResponse<FixedActivityScheduleResponse>> resolveFixedActivity(
            @PathVariable Long yearId, @RequestBody FixedActivityResolveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fixed activity schedule",
                service.resolveFixedActivitySchedule(yearId, request)));
    }

    @GetMapping("/years/{yearId}/timetable/fixed-activities")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<List<FixedActivityResponse>>> fixedActivities(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Fixed activities",
                service.listFixedActivities(yearId)));
    }

    @PostMapping("/years/{yearId}/timetable/fixed-activities")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<FixedActivityResponse>> createFixedActivity(
            @PathVariable Long yearId, @Valid @RequestBody FixedActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                "Fixed activity created", service.createFixedActivity(yearId, request)));
    }

    @PutMapping("/timetable/fixed-activities/{activityId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<FixedActivityResponse>> updateFixedActivity(
            @PathVariable Long activityId, @Valid @RequestBody FixedActivityRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fixed activity updated",
                service.updateFixedActivity(activityId, request)));
    }

    @PostMapping("/timetable/fixed-activities/{activityId}/activate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<FixedActivityResponse>> activateFixedActivity(@PathVariable Long activityId) {
        return ResponseEntity.ok(ApiResponse.success("Fixed activity activated",
                service.setFixedActivityActive(activityId, true)));
    }

    @PostMapping("/timetable/fixed-activities/{activityId}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<FixedActivityResponse>> deactivateFixedActivity(@PathVariable Long activityId) {
        return ResponseEntity.ok(ApiResponse.success("Fixed activity deactivated",
                service.setFixedActivityActive(activityId, false)));
    }

    @DeleteMapping("/timetable/fixed-activities/{activityId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteFixedActivity(@PathVariable Long activityId) {
        service.deleteFixedActivity(activityId);
        return ResponseEntity.ok(ApiResponse.success("Fixed activity removed", null));
    }
}
