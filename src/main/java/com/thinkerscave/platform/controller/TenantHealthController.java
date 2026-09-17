package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.response.*;
import com.thinkerscave.platform.service.TenantHealthService;
import com.thinkerscave.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/tenant-health")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class TenantHealthController {
    private final TenantHealthService healthService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TenantHealthSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Tenant health summary retrieved", healthService.summary()));
    }

    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<TenantHealthPageResponse>> tenants(
            @RequestParam(required = false) String search, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Tenant health retrieved",
                healthService.tenants(search, pageable)));
    }

    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<ApiResponse<TenantHealthResponse>> detail(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Tenant health detail retrieved",
                healthService.check(tenantId)));
    }
}
