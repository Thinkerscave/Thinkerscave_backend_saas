package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.response.PlatformDashboardResponse;
import com.thinkerscave.platform.service.PlatformDashboardService;
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
@RequestMapping("/api/platform/dashboard")
@RequiredArgsConstructor
@Tag(name = "Platform Dashboard", description = "Platform-wide KPI and statistics")
public class PlatformDashboardController {

    private final PlatformDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get platform-wide dashboard KPIs")
    public ResponseEntity<ApiResponse<PlatformDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved", dashboardService.getDashboard()));
    }
}
