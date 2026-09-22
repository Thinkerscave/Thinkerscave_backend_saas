package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.response.TenantHealthResponse;
import com.thinkerscave.platform.service.TenantHealthService;
import com.thinkerscave.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/platform/operations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class TenantOperationsController {
    private final TenantHealthService healthService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<List<TenantHealthResponse>>> health() {
        return ResponseEntity.ok(ApiResponse.success("Tenant health retrieved", healthService.checkAll()));
    }

    @GetMapping("/health/{tenantId}")
    public ResponseEntity<ApiResponse<TenantHealthResponse>> health(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Tenant health retrieved", healthService.check(tenantId)));
    }

    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> audit(
            @RequestParam(required = false) String tenant,
            @RequestParam(required = false) Long executionId,
            @RequestParam(required = false) String eventType) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM operation_audit_events
                WHERE (CAST(? AS VARCHAR) IS NULL OR tenant_identifier=?)
                  AND (CAST(? AS BIGINT) IS NULL OR execution_id=?)
                  AND (CAST(? AS VARCHAR) IS NULL OR event_type=?)
                ORDER BY created_on DESC LIMIT 500
                """, tenant, tenant, executionId, executionId, eventType, eventType);
        return ResponseEntity.ok(ApiResponse.success("Operation audit retrieved", rows));
    }

    @GetMapping("/catalog/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> catalogHistory(
            @RequestParam(required = false) Long tenantId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT e.*, c.version_number AS catalog_version, c.change_type,
                       c.entity_type, c.entity_key, t.tenant_identifier
                FROM catalog_sync_executions e
                JOIN catalog_versions c ON c.id=e.catalog_version_id
                JOIN tenant_registry t ON t.id=e.tenant_registry_id
                WHERE (CAST(? AS BIGINT) IS NULL OR e.tenant_registry_id=?)
                ORDER BY e.created_on DESC LIMIT 500
                """, tenantId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Catalog sync history retrieved", rows));
    }

}
