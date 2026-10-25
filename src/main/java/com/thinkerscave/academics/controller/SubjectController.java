package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.SubjectResponse;
import com.thinkerscave.academics.dto.response.SubjectUsageResponse;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.service.SubjectService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academics/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Reusable subject catalog")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Subject directory with search, type and status filters")
    public ResponseEntity<ApiResponse<PageResponse<SubjectResponse>>> directory(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SubjectType subjectType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Subjects retrieved",
                subjectService.getDirectory(q, subjectType, parseStatus(status), page, size)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create a reusable subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> create(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Subject created", subjectService.create(request)));
    }

    @GetMapping("/{subjectId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Subject details with usage summary")
    public ResponseEntity<ApiResponse<SubjectResponse>> getById(@PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success("Subject found", subjectService.getById(subjectId)));
    }

    @PutMapping("/{subjectId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update catalog attributes of a subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> update(
            @PathVariable Long subjectId, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subject updated", subjectService.update(subjectId, request)));
    }

    @PatchMapping("/{subjectId}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Deactivate a subject (existing mappings are kept)")
    public ResponseEntity<ApiResponse<SubjectResponse>> deactivate(@PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success("Subject deactivated", subjectService.deactivate(subjectId)));
    }

    @PatchMapping("/{subjectId}/activate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Reactivate a subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> activate(@PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success("Subject activated", subjectService.activate(subjectId)));
    }

    @GetMapping("/{subjectId}/mappings")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Academic groups where a subject is currently mapped")
    public ResponseEntity<ApiResponse<PageResponse<SubjectUsageResponse>>> usages(
            @PathVariable Long subjectId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Subject usages retrieved",
                subjectService.getUsages(subjectId, q, page, size)));
    }

    private Boolean parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        return switch (status.trim().toUpperCase()) {
            case "ACTIVE" -> Boolean.TRUE;
            case "INACTIVE" -> Boolean.FALSE;
            default -> throw new BadRequestException("Status must be ACTIVE or INACTIVE");
        };
    }
}
