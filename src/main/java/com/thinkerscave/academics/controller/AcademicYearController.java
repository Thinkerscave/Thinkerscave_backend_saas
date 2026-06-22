package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.AcademicYearRequest;
import com.thinkerscave.academics.dto.request.CloneAcademicYearRequest;
import com.thinkerscave.academics.dto.response.AcademicYearResponse;
import com.thinkerscave.academics.service.AcademicYearService;
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
@RequestMapping("/api/v1/academics/years")
@RequiredArgsConstructor
@Tag(name = "Academic Year", description = "Manage academic years")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Create academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> create(@Valid @RequestBody AcademicYearRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Academic year created", academicYearService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Update academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> update(
            @PathVariable Long id, @Valid @RequestBody AcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Academic year updated", academicYearService.update(id, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get academic year by ID")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Academic year found", academicYearService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get all academic years")
    public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Academic years retrieved", academicYearService.getAll()));
    }

    @PatchMapping("/{id}/set-current")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Set academic year as current")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> setCurrent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Current academic year set", academicYearService.setCurrentYear(id)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Deactivate academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Academic year deactivated", academicYearService.deactivate(id)));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Clone academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> clone(
            @PathVariable Long id, @Valid @RequestBody CloneAcademicYearRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Academic year cloned", academicYearService.clone(id, request)));
    }
}
