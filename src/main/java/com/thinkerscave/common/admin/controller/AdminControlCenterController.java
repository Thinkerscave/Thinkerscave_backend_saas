package com.thinkerscave.common.admin.controller;

import com.thinkerscave.common.admin.dto.AdminControlCenterDTO;
import com.thinkerscave.common.admin.service.AdminControlCenterService;
import com.thinkerscave.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin-control")
@Tag(name = "Administration Control Center", description = "Aggregated APIs for the ERP administration control tower")
@RequiredArgsConstructor
public class AdminControlCenterController {

    private final AdminControlCenterService adminControlCenterService;

    @GetMapping("/workspace")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Load Administration control center workspace")
    public ResponseEntity<ApiResponse<AdminControlCenterDTO>> workspace() {
        return ResponseEntity.ok(ApiResponse.success("Administration workspace loaded", adminControlCenterService.getWorkspace()));
    }

    @PostMapping("/diagnostics")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Run system diagnostics and persist the result")
    public ResponseEntity<ApiResponse<AdminControlCenterDTO.SystemEventDTO>> runDiagnostics() {
        return ResponseEntity.ok(ApiResponse.success("System diagnostics completed", adminControlCenterService.runDiagnostics()));
    }
}