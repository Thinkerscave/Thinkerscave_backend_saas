package com.thinkerscave.dashboard.controller;

import com.thinkerscave.dashboard.dto.DashboardSearchResponseDTO;
import com.thinkerscave.dashboard.dto.DashboardSummaryDTO;
import com.thinkerscave.dashboard.dto.response.DashboardResponse;
import com.thinkerscave.dashboard.service.DashboardOrchestrationService;
import com.thinkerscave.dashboard.service.DashboardSearchService;
import com.thinkerscave.dashboard.service.DashboardService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Role-aware KPI dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardSearchService dashboardSearchService;
    private final DashboardOrchestrationService dashboardOrchestrationService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF', 'TEACHER', 'PRINCIPAL', 'HR_MANAGER')")
    @Operation(summary = "Aggregated KPI summary for the current organization")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary", dashboardService.getSummary()));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'PLATFORM_ADMIN', 'THINKERSCAVE_INTERNAL', 'INTERNAL_TEAM', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF', 'TEACHER', 'PRINCIPAL', 'HR_MANAGER', 'RECEPTIONIST', 'ACADEMIC_COORDINATOR', 'ADMIN')")
    @Operation(summary = "Role-aware global search")
    public ResponseEntity<ApiResponse<DashboardSearchResponseDTO>> search(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.success("Search results", dashboardSearchService.search(query)));
    }

    @GetMapping("/workspace")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Backend-driven, role-based dashboard workspace: dashboard type + fully composed widget list")
    public ResponseEntity<ApiResponse<DashboardResponse>> workspace() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard workspace retrieved", dashboardOrchestrationService.getWorkspace()));
    }
}
