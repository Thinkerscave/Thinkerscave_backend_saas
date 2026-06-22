package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.AcademicScheduleRequest;
import com.thinkerscave.academics.dto.request.ClassScheduleAssignmentRequest;
import com.thinkerscave.academics.dto.request.PeriodTemplateRequest;
import com.thinkerscave.academics.dto.request.TimetableTemplateRequest;
import com.thinkerscave.academics.dto.response.AcademicScheduleResponse;
import com.thinkerscave.academics.dto.response.PeriodTemplateResponse;
import com.thinkerscave.academics.dto.response.TimetableTemplateResponse;
import com.thinkerscave.academics.service.AcademicScheduleService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academics/schedules")
@RequiredArgsConstructor
@Tag(name = "Academic Schedule", description = "Manage schedules, timetable templates and periods")
public class AcademicScheduleController {

    private final AcademicScheduleService scheduleService;

    // ---- Schedule ----

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Create academic schedule")
    public ResponseEntity<ApiResponse<AcademicScheduleResponse>> create(@Valid @RequestBody AcademicScheduleRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Schedule created", scheduleService.createSchedule(request)));
    }

    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Update academic schedule")
    public ResponseEntity<ApiResponse<AcademicScheduleResponse>> update(
            @PathVariable Long scheduleId, @Valid @RequestBody AcademicScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Schedule updated", scheduleService.updateSchedule(scheduleId, request)));
    }

    @GetMapping("/{scheduleId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<ApiResponse<AcademicScheduleResponse>> getById(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(ApiResponse.success("Schedule found", scheduleService.getScheduleById(scheduleId)));
    }

    @GetMapping("/year/{yearId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get schedules by academic year")
    public ResponseEntity<ApiResponse<List<AcademicScheduleResponse>>> getByYear(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Schedules retrieved", scheduleService.getSchedulesByYear(yearId)));
    }

    @PatchMapping("/{scheduleId}/deactivate")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Deactivate schedule")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long scheduleId) {
        scheduleService.deactivateSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Schedule deactivated", null));
    }

    // ---- Template ----

    @PostMapping("/{scheduleId}/templates")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Create timetable template")
    public ResponseEntity<ApiResponse<TimetableTemplateResponse>> createTemplate(
            @PathVariable Long scheduleId, @Valid @RequestBody TimetableTemplateRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Template created", scheduleService.createTemplate(scheduleId, request)));
    }

    @PutMapping("/templates/{templateId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Update timetable template")
    public ResponseEntity<ApiResponse<TimetableTemplateResponse>> updateTemplate(
            @PathVariable Long templateId, @Valid @RequestBody TimetableTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Template updated", scheduleService.updateTemplate(templateId, request)));
    }

    @GetMapping("/{scheduleId}/templates")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get templates by schedule")
    public ResponseEntity<ApiResponse<List<TimetableTemplateResponse>>> getTemplates(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(ApiResponse.success("Templates retrieved", scheduleService.getTemplatesBySchedule(scheduleId)));
    }

    @PatchMapping("/templates/{templateId}/deactivate")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Deactivate timetable template")
    public ResponseEntity<ApiResponse<Void>> deactivateTemplate(@PathVariable Long templateId) {
        scheduleService.deactivateTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("Template deactivated", null));
    }

    // ---- Period ----

    @PostMapping("/templates/{templateId}/periods")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Add period to template")
    public ResponseEntity<ApiResponse<PeriodTemplateResponse>> addPeriod(
            @PathVariable Long templateId, @Valid @RequestBody PeriodTemplateRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Period added", scheduleService.addPeriod(templateId, request)));
    }

    @PutMapping("/periods/{periodId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Update period")
    public ResponseEntity<ApiResponse<PeriodTemplateResponse>> updatePeriod(
            @PathVariable Long periodId, @Valid @RequestBody PeriodTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Period updated", scheduleService.updatePeriod(periodId, request)));
    }

    @GetMapping("/templates/{templateId}/periods")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get periods by template")
    public ResponseEntity<ApiResponse<List<PeriodTemplateResponse>>> getPeriods(@PathVariable Long templateId) {
        return ResponseEntity.ok(ApiResponse.success("Periods retrieved", scheduleService.getPeriodsByTemplate(templateId)));
    }

    @DeleteMapping("/periods/{periodId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete period")
    public ResponseEntity<ApiResponse<Void>> deletePeriod(@PathVariable Long periodId) {
        scheduleService.deletePeriod(periodId);
        return ResponseEntity.ok(ApiResponse.success("Period deleted", null));
    }

    // ---- Class-schedule assignment ----

    @PostMapping("/assign-class")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Assign schedule to class")
    public ResponseEntity<ApiResponse<Void>> assignToClass(@Valid @RequestBody ClassScheduleAssignmentRequest request) {
        scheduleService.assignScheduleToClass(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Schedule assigned to class", null));
    }

    @DeleteMapping("/class-assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Remove schedule from class")
    public ResponseEntity<ApiResponse<Void>> removeFromClass(@PathVariable Long assignmentId) {
        scheduleService.removeScheduleFromClass(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Class schedule assignment removed", null));
    }
}
