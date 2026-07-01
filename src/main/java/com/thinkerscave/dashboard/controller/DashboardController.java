package com.thinkerscave.dashboard.controller;

import com.thinkerscave.dashboard.dto.DashboardSummaryDTO;
import com.thinkerscave.dashboard.service.DashboardService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Role-aware KPI dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    @Operation(summary = "Aggregated KPI summary for the current organization")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary", dashboardService.getSummary()));
    }
}
