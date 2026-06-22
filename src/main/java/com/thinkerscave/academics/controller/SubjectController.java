package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.SubjectResponse;
import com.thinkerscave.academics.service.SubjectService;
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
@RequestMapping("/api/v1/academics/subjects")
@RequiredArgsConstructor
@Tag(name = "Subject", description = "Manage subjects")
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Create subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> create(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Subject created", subjectService.create(request)));
    }

    @PutMapping("/{subjectId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Update subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> update(
            @PathVariable Long subjectId, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subject updated", subjectService.update(subjectId, request)));
    }

    @GetMapping("/{subjectId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get subject by ID")
    public ResponseEntity<ApiResponse<SubjectResponse>> getById(@PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success("Subject found", subjectService.getById(subjectId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get all active subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Subjects retrieved", subjectService.getAll()));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Search subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success("Search results", subjectService.search(keyword)));
    }

    @PatchMapping("/{subjectId}/deactivate")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Deactivate subject")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long subjectId) {
        subjectService.deactivate(subjectId);
        return ResponseEntity.ok(ApiResponse.success("Subject deactivated", null));
    }
}
