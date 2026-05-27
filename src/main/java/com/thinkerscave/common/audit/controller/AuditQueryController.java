package com.thinkerscave.common.audit.controller;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.dto.AuditLogDTO;
import com.thinkerscave.common.audit.dto.SecurityAuditLogDTO;
import com.thinkerscave.common.audit.service.AuditQueryService;
import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Audit log query API")
@RequiredArgsConstructor
@Slf4j
public class AuditQueryController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/logs")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('AUDIT_VIEW')")
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
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                auditQueryService.searchAudit(eventType, entityType, entityId, actorUserId, from, to,
                        PageRequestUtil.of(page, size, sort)))));
    }

    @GetMapping("/entity-history")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('AUDIT_VIEW')")
    @Operation(summary = "All audit entries for a single entity instance")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> entityHistory(
            @RequestParam String entityType,
            @RequestParam String entityId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                auditQueryService.entityHistory(entityType, entityId,
                        PageRequestUtil.of(page, size, sort)))));
    }

    @GetMapping("/security")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('AUDIT_SECURITY_VIEW')")
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
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                auditQueryService.searchSecurity(username, eventCode, success, from, to,
                        PageRequestUtil.of(page, size, sort)))));
    }
}
