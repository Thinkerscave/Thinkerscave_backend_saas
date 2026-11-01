package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.SubjectChoiceGroupRequest;
import com.thinkerscave.academics.dto.response.SubjectChoiceGroupResponse;
import com.thinkerscave.academics.dto.response.SubjectOptionResponse;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.service.SubjectChoiceGroupService;
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
@RequestMapping("/api/v1/academics/subject-choice-groups")
@RequiredArgsConstructor
@Tag(name = "Subject Choice Groups", description = "Reusable groups of alternative subjects")
public class SubjectChoiceGroupController {

    private final SubjectChoiceGroupService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Choice group directory with search and filters")
    public ResponseEntity<ApiResponse<PageResponse<SubjectChoiceGroupResponse>>> directory(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SubjectType subjectType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Choice groups retrieved",
                service.getDirectory(q, subjectType, parseStatus(status), page, size)));
    }

    @GetMapping("/subject-options")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Active subjects eligible for a choice-group membership")
    public ResponseEntity<ApiResponse<PageResponse<SubjectOptionResponse>>> subjectOptions(
            @RequestParam SubjectType subjectType,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Subject options retrieved",
                service.getSubjectOptions(q, subjectType, page, size)));
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<SubjectChoiceGroupResponse>> getById(@PathVariable Long groupId) {
        return ResponseEntity.ok(ApiResponse.success("Choice group retrieved", service.getById(groupId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SubjectChoiceGroupResponse>> create(
            @Valid @RequestBody SubjectChoiceGroupRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Choice group created", service.create(request)));
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SubjectChoiceGroupResponse>> update(
            @PathVariable Long groupId, @Valid @RequestBody SubjectChoiceGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Choice group updated", service.update(groupId, request)));
    }

    @PatchMapping("/{groupId}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SubjectChoiceGroupResponse>> deactivate(@PathVariable Long groupId) {
        return ResponseEntity.ok(ApiResponse.success("Choice group deactivated", service.deactivate(groupId)));
    }

    @PatchMapping("/{groupId}/activate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SubjectChoiceGroupResponse>> activate(@PathVariable Long groupId) {
        return ResponseEntity.ok(ApiResponse.success("Choice group activated", service.activate(groupId)));
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
