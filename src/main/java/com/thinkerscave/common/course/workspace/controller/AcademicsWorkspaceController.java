package com.thinkerscave.common.course.workspace.controller;

import com.thinkerscave.common.course.workspace.dto.AcademicsWorkspaceDtos.*;
import com.thinkerscave.common.course.workspace.service.AcademicsWorkspaceService;
import com.thinkerscave.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/academics/workspace")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRINCIPAL', 'TEACHER', 'STAFF', 'RECEPTIONIST')")
public class AcademicsWorkspaceController {

    private final AcademicsWorkspaceService service;

    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<AcademicsKpi>> kpi() {
        return ResponseEntity.ok(ApiResponse.success(service.kpi()));
    }

    @GetMapping("/structure")
    public ResponseEntity<ApiResponse<AcademicsStructure>> structure() {
        return ResponseEntity.ok(ApiResponse.success(service.structure()));
    }
}
