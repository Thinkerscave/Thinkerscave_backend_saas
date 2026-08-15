package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.AcademicYearTransitionRequest;
import com.thinkerscave.academics.dto.response.AcademicYearTransitionResponse;
import com.thinkerscave.academics.service.AcademicYearTransitionService;
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
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
@Tag(name = "Academic Year Transition", description = "Copy academic structure between years")
public class AcademicYearTransitionController {

    private final AcademicYearTransitionService transitionService;

    @PostMapping("/years/{sourceYearId}/transitions")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create a transition record between source and target academic years")
    public ResponseEntity<ApiResponse<AcademicYearTransitionResponse>> create(
            @PathVariable Long sourceYearId,
            @Valid @RequestBody AcademicYearTransitionRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                "Transition created", transitionService.create(sourceYearId, request)));
    }

    @GetMapping("/years/{yearId}/transitions")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "List transitions for an academic year (as source or target)")
    public ResponseEntity<ApiResponse<List<AcademicYearTransitionResponse>>> list(
            @PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Transitions retrieved", transitionService.listByYear(yearId)));
    }

    @PostMapping("/transitions/{id}/start")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Start a transition — copies selected structure into target year")
    public ResponseEntity<ApiResponse<AcademicYearTransitionResponse>> start(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Transition started", transitionService.start(id)));
    }

    @PostMapping("/transitions/{id}/approve")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Approve a completed transition")
    public ResponseEntity<ApiResponse<AcademicYearTransitionResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Transition approved", transitionService.approve(id)));
    }
}
