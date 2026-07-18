package com.thinkerscave.security.controller;

import com.thinkerscave.security.dto.request.WorkspaceSwitchRequest;
import com.thinkerscave.security.dto.response.WorkspaceOrganizationResponse;
import com.thinkerscave.security.service.WorkspaceAccessService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspace Access", description = "Organization owner workspace discovery and switching")
public class WorkspaceController {

    private final WorkspaceAccessService workspaceAccessService;

    @GetMapping("/organizations")
    @PreAuthorize("hasAuthority('ORGANIZATION_OWNER')")
    @Operation(summary = "List all active organizations owned by the logged-in customer owner")
    public ResponseEntity<ApiResponse<List<WorkspaceOrganizationResponse>>> organizations() {
        return ResponseEntity.ok(ApiResponse.success(
                "Owned organizations loaded",
                workspaceAccessService.getOwnedOrganizations()));
    }

    @PostMapping("/switch")
    @PreAuthorize("hasAuthority('ORGANIZATION_OWNER')")
    @Operation(summary = "Validate and switch active workspace context without re-login")
    public ResponseEntity<ApiResponse<WorkspaceOrganizationResponse>> switchWorkspace(
            @Valid @RequestBody WorkspaceSwitchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Workspace switched",
                workspaceAccessService.switchWorkspace(request.getOrganizationId())));
    }
}
