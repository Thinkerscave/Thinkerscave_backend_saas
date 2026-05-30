package com.thinkerscave.common.dashboard.controller;

import com.thinkerscave.common.dashboard.dto.DashboardSummaryDTO;
import com.thinkerscave.common.dashboard.dto.DashboardSearchDTO;
import com.thinkerscave.common.dashboard.dto.DashboardWorkspaceDTO;
import com.thinkerscave.common.dashboard.service.DashboardService;
import com.thinkerscave.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Role-aware workspace for the organization dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('DASHBOARD_VIEW')")
    @Operation(summary = "Aggregated KPI counts for the current organization")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> summary(
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.summary(academicYearId)));
    }

    @GetMapping("/workspace")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Role-aware dashboard workspace for the signed-in user")
    public ResponseEntity<ApiResponse<DashboardWorkspaceDTO>> workspace() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.workspace()));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Global dashboard search across school records")
    public ResponseEntity<ApiResponse<DashboardSearchDTO>> search(@RequestParam("query") String query) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.search(query)));
    }
}
