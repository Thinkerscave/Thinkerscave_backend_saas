package com.thinkerscave.common.course.controller;

import com.thinkerscave.common.course.dto.AcademicCalendarEventDTO;
import com.thinkerscave.common.course.dto.AcademicSettingDTO;
import com.thinkerscave.common.course.dto.ClassTeacherAssignmentDTO;
import com.thinkerscave.common.course.dto.TimetableSlotDTO;
import com.thinkerscave.common.course.service.AcademicWorkspaceOperationsService;
import com.thinkerscave.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
@Tag(name = "Premium Academics Workspace", description = "Phase 1 academic workspace operations")
public class AcademicWorkspaceOperationsController {

    private final AcademicWorkspaceOperationsService operationsService;

    @GetMapping("/class-teachers")
    @Operation(summary = "List class teacher assignments")
    public ResponseEntity<ApiResponse<List<ClassTeacherAssignmentDTO>>> listClassTeacherAssignments(
            @RequestParam Long organizationId,
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Class teacher assignments fetched successfully",
                operationsService.listClassTeacherAssignments(organizationId, academicYearId)));
    }

    @PostMapping("/class-teachers")
    @Operation(summary = "Create class teacher assignment")
    public ResponseEntity<ApiResponse<ClassTeacherAssignmentDTO>> createClassTeacherAssignment(@RequestBody ClassTeacherAssignmentDTO dto) {
        return ResponseEntity.ok(ApiResponse.created("Class teacher assignment saved successfully",
                operationsService.saveClassTeacherAssignment(dto)));
    }

    @PutMapping("/class-teachers/{id}")
    @Operation(summary = "Update class teacher assignment")
    public ResponseEntity<ApiResponse<ClassTeacherAssignmentDTO>> updateClassTeacherAssignment(
            @PathVariable Long id,
            @RequestBody ClassTeacherAssignmentDTO dto) {
        dto.setAssignmentId(id);
        return ResponseEntity.ok(ApiResponse.success("Class teacher assignment updated successfully",
                operationsService.saveClassTeacherAssignment(dto)));
    }

    @DeleteMapping("/class-teachers/{id}")
    @Operation(summary = "Deactivate class teacher assignment")
    public ResponseEntity<ApiResponse<Void>> deactivateClassTeacherAssignment(
            @PathVariable Long id,
            @RequestParam Long organizationId) {
        operationsService.deactivateClassTeacherAssignment(organizationId, id);
        return ResponseEntity.ok(ApiResponse.noContent("Class teacher assignment deactivated successfully"));
    }

    @GetMapping("/timetable-slots")
    @Operation(summary = "List timetable slots")
    public ResponseEntity<ApiResponse<List<TimetableSlotDTO>>> listTimetableSlots(
            @RequestParam Long organizationId,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long teacherId) {
        return ResponseEntity.ok(ApiResponse.success("Timetable slots fetched successfully",
                operationsService.listTimetableSlots(organizationId, academicYearId, classId, teacherId)));
    }

    @PostMapping("/timetable-slots")
    @Operation(summary = "Create timetable slot")
    public ResponseEntity<ApiResponse<TimetableSlotDTO>> createTimetableSlot(@RequestBody TimetableSlotDTO dto) {
        return ResponseEntity.ok(ApiResponse.created("Timetable slot saved successfully",
                operationsService.saveTimetableSlot(dto)));
    }

    @PutMapping("/timetable-slots/{id}")
    @Operation(summary = "Update timetable slot")
    public ResponseEntity<ApiResponse<TimetableSlotDTO>> updateTimetableSlot(
            @PathVariable Long id,
            @RequestBody TimetableSlotDTO dto) {
        dto.setSlotId(id);
        return ResponseEntity.ok(ApiResponse.success("Timetable slot updated successfully",
                operationsService.saveTimetableSlot(dto)));
    }

    @DeleteMapping("/timetable-slots/{id}")
    @Operation(summary = "Deactivate timetable slot")
    public ResponseEntity<ApiResponse<Void>> deactivateTimetableSlot(
            @PathVariable Long id,
            @RequestParam Long organizationId) {
        operationsService.deactivateTimetableSlot(organizationId, id);
        return ResponseEntity.ok(ApiResponse.noContent("Timetable slot deactivated successfully"));
    }

    @GetMapping("/calendar-events")
    @Operation(summary = "List academic calendar events")
    public ResponseEntity<ApiResponse<List<AcademicCalendarEventDTO>>> listCalendarEvents(
            @RequestParam Long organizationId,
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Academic calendar events fetched successfully",
                operationsService.listCalendarEvents(organizationId, academicYearId)));
    }

    @PostMapping("/calendar-events")
    @Operation(summary = "Create academic calendar event")
    public ResponseEntity<ApiResponse<AcademicCalendarEventDTO>> createCalendarEvent(@RequestBody AcademicCalendarEventDTO dto) {
        return ResponseEntity.ok(ApiResponse.created("Academic calendar event saved successfully",
                operationsService.saveCalendarEvent(dto)));
    }

    @PutMapping("/calendar-events/{id}")
    @Operation(summary = "Update academic calendar event")
    public ResponseEntity<ApiResponse<AcademicCalendarEventDTO>> updateCalendarEvent(
            @PathVariable Long id,
            @RequestBody AcademicCalendarEventDTO dto) {
        dto.setEventId(id);
        return ResponseEntity.ok(ApiResponse.success("Academic calendar event updated successfully",
                operationsService.saveCalendarEvent(dto)));
    }

    @DeleteMapping("/calendar-events/{id}")
    @Operation(summary = "Deactivate academic calendar event")
    public ResponseEntity<ApiResponse<Void>> deactivateCalendarEvent(
            @PathVariable Long id,
            @RequestParam Long organizationId) {
        operationsService.deactivateCalendarEvent(organizationId, id);
        return ResponseEntity.ok(ApiResponse.noContent("Academic calendar event deactivated successfully"));
    }

    @GetMapping("/settings")
    @Operation(summary = "List academic settings")
    public ResponseEntity<ApiResponse<List<AcademicSettingDTO>>> listSettings(@RequestParam Long organizationId) {
        return ResponseEntity.ok(ApiResponse.success("Academic settings fetched successfully",
                operationsService.listAcademicSettings(organizationId)));
    }

    @PostMapping("/settings")
    @Operation(summary = "Create or update academic setting")
    public ResponseEntity<ApiResponse<AcademicSettingDTO>> saveSetting(@RequestBody AcademicSettingDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Academic setting saved successfully",
                operationsService.saveAcademicSetting(dto)));
    }

    @PutMapping("/settings/{id}")
    @Operation(summary = "Update academic setting")
    public ResponseEntity<ApiResponse<AcademicSettingDTO>> updateSetting(
            @PathVariable Long id,
            @RequestBody AcademicSettingDTO dto) {
        dto.setSettingId(id);
        return ResponseEntity.ok(ApiResponse.success("Academic setting updated successfully",
                operationsService.saveAcademicSetting(dto)));
    }
}