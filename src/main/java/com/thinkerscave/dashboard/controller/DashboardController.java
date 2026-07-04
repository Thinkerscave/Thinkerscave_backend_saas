package com.thinkerscave.dashboard.controller;

import com.thinkerscave.dashboard.dto.DashboardSearchResponseDTO;
import com.thinkerscave.dashboard.dto.DashboardSummaryDTO;
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

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF', 'TEACHER', 'PRINCIPAL', 'HR_MANAGER')")
    @Operation(summary = "Aggregated KPI summary for the current organization")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary", dashboardService.getSummary()));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF', 'TEACHER', 'PRINCIPAL', 'HR_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Global search across students, staff and admission leads")
    public ResponseEntity<ApiResponse<DashboardSearchResponseDTO>> search(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.success("Search results", dashboardSearchService.search(query)));
    }
}
