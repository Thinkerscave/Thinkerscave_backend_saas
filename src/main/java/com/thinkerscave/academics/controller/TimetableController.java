package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.TimetableSlotRequest;
import com.thinkerscave.academics.dto.response.TimetableResponse;
import com.thinkerscave.academics.dto.response.TimetableSlotResponse;
import com.thinkerscave.academics.service.TimetableService;
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
@RequestMapping("/api/v1/academics/timetable")
@RequiredArgsConstructor
@Tag(name = "Timetable", description = "Manage timetable slots")
public class TimetableController {

    private final TimetableService timetableService;

    @PostMapping("/slots")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Create timetable slot")
    public ResponseEntity<ApiResponse<TimetableSlotResponse>> createSlot(@Valid @RequestBody TimetableSlotRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Timetable slot created", timetableService.createSlot(request)));
    }

    @PutMapping("/slots/{slotId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Update timetable slot")
    public ResponseEntity<ApiResponse<TimetableSlotResponse>> updateSlot(
            @PathVariable Long slotId, @Valid @RequestBody TimetableSlotRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Timetable slot updated", timetableService.updateSlot(slotId, request)));
    }

    @DeleteMapping("/slots/{slotId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete timetable slot")
    public ResponseEntity<ApiResponse<Void>> deleteSlot(@PathVariable Long slotId) {
        timetableService.deleteSlot(slotId);
        return ResponseEntity.ok(ApiResponse.success("Timetable slot deleted", null));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "Get timetable for a class/section")
    public ResponseEntity<ApiResponse<TimetableResponse>> getTimetable(
            @PathVariable Long classId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("Timetable retrieved", timetableService.getTimetableForClass(classId, sectionId)));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get timetable for a teacher")
    public ResponseEntity<ApiResponse<List<TimetableSlotResponse>>> getTeacherTimetable(
            @PathVariable Long teacherId,
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Teacher timetable retrieved", timetableService.getTeacherTimetable(teacherId, academicYearId)));
    }
}
