package com.thinkerscave.audit.controller;

import com.thinkerscave.audit.dto.AuditLogDTO;
import com.thinkerscave.audit.dto.SecurityAuditLogDTO;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditQueryService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Audit log query API")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/logs")
    @Operation(summary = "Filtered audit log search")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> search(
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Audit logs",
                auditQueryService.searchAudit(eventType, entityType, entityId, actorUserId, from, to, page, size, sort)));
    }

    @GetMapping("/entity-history")
    @Operation(summary = "All audit entries for a single entity instance")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> entityHistory(
            @RequestParam String entityType,
            @RequestParam String entityId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Entity history",
                auditQueryService.entityHistory(entityType, entityId, page, size, sort)));
    }

    @GetMapping("/security")
    @Operation(summary = "Filtered security audit log search")
    public ResponseEntity<ApiResponse<PageResponse<SecurityAuditLogDTO>>> security(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String eventCode,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Security audit logs",
                auditQueryService.searchSecurity(username, eventCode, success, from, to, page, size, sort)));
    }
}
