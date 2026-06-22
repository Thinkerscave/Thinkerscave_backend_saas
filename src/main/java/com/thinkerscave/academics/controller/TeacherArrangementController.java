package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.TeacherArrangementRequest;
import com.thinkerscave.academics.dto.response.TeacherArrangementResponse;
import com.thinkerscave.academics.service.TeacherArrangementService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/academics/arrangements")
@RequiredArgsConstructor
@Tag(name = "Teacher Arrangement", description = "Manage teacher substitution arrangements")
public class TeacherArrangementController {

    private final TeacherArrangementService arrangementService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR')")
    @Operation(summary = "Create teacher arrangement")
    public ResponseEntity<ApiResponse<TeacherArrangementResponse>> create(@Valid @RequestBody TeacherArrangementRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Arrangement created", arrangementService.create(request)));
    }

    @GetMapping("/{arrangementId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get arrangement by ID")
    public ResponseEntity<ApiResponse<TeacherArrangementResponse>> getById(@PathVariable Long arrangementId) {
        return ResponseEntity.ok(ApiResponse.success("Arrangement found", arrangementService.getById(arrangementId)));
    }

    @GetMapping("/by-date")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR')")
    @Operation(summary = "Get arrangements by date")
    public ResponseEntity<ApiResponse<List<TeacherArrangementResponse>>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success("Arrangements retrieved", arrangementService.getByDate(date)));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get arrangements by teacher")
    public ResponseEntity<ApiResponse<List<TeacherArrangementResponse>>> getByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(ApiResponse.success("Arrangements retrieved", arrangementService.getByTeacher(teacherId)));
    }

    @PatchMapping("/{arrangementId}/approve")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Approve arrangement")
    public ResponseEntity<ApiResponse<TeacherArrangementResponse>> approve(
            @PathVariable Long arrangementId,
            @RequestParam Long approvedBy) {
        return ResponseEntity.ok(ApiResponse.success("Arrangement approved", arrangementService.approve(arrangementId, approvedBy)));
    }

    @PatchMapping("/{arrangementId}/reject")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Reject arrangement")
    public ResponseEntity<ApiResponse<TeacherArrangementResponse>> reject(@PathVariable Long arrangementId) {
        return ResponseEntity.ok(ApiResponse.success("Arrangement rejected", arrangementService.reject(arrangementId)));
    }
}
