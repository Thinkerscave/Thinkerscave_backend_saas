package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.response.LoginHistoryResponse;
import com.thinkerscave.access.enums.LoginStatus;
import com.thinkerscave.access.service.LoginHistoryService;
import com.thinkerscave.retention.LoginHistoryRetentionTask;
import com.thinkerscave.retention.RetentionTrigger;
import com.thinkerscave.retention.dto.RetentionPurgeResult;
import com.thinkerscave.retention.dto.RetentionTaskStatus;
import com.thinkerscave.retention.service.RetentionService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/access/login-history")
@RequiredArgsConstructor
@Tag(name = "Login History", description = "Recent sign-ins. Events older than 30 days are deleted.")
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;
    private final RetentionService retentionService;

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get login history for a specific user")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Page<LoginHistoryResponse>>> getUserHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) LoginStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("loginTime").descending());
        return ResponseEntity.ok(ApiResponse.success(loginHistoryService.getUserLoginHistory(
                userId,
                status,
                toLocal(from),
                toLocal(to),
                search,
                pageable)));
    }

    @GetMapping("/organizations/{organizationId}")
    @Operation(summary = "Get login history for all users in an organization")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Page<LoginHistoryResponse>>> getOrgHistory(
            @PathVariable Long organizationId,
            @RequestParam(required = false) LoginStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        assertOrganizationInScope(organizationId);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("loginTime").descending());
        return ResponseEntity.ok(ApiResponse.success(
                loginHistoryService.getOrganizationLoginHistory(
                        organizationId,
                        status,
                        toLocal(from),
                        toLocal(to),
                        search,
                        pageable)));
    }

    @GetMapping("/organizations/{organizationId}/retention")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    @Operation(summary = "Last login-history cleanup for this school")
    public ResponseEntity<ApiResponse<RetentionTaskStatus>> getRetention(@PathVariable Long organizationId) {
        assertOrganizationInScope(organizationId);
        return ResponseEntity.ok(ApiResponse.success(retentionService.getTask(LoginHistoryRetentionTask.KEY)));
    }

    @PostMapping("/organizations/{organizationId}/purge")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    @Operation(summary = "Delete this school's login history older than 30 days")
    public ResponseEntity<ApiResponse<RetentionPurgeResult>> purge(@PathVariable Long organizationId) {
        assertOrganizationInScope(organizationId);
        RetentionPurgeResult result = retentionService.run(
                LoginHistoryRetentionTask.KEY,
                RetentionTrigger.MANUAL,
                organizationId,
                currentUsername());
        return ResponseEntity.ok(ApiResponse.success(result.getSummary(), result));
    }

    private LocalDateTime toLocal(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private void assertOrganizationInScope(Long organizationId) {
        if (hasAuthority("SUPER_ADMIN")) {
            return;
        }
        Long currentOrg = OrganizationContext.getOrganizationId();
        if (currentOrg == null || !currentOrg.equals(organizationId)) {
            throw new AccessDeniedException("Not authorized for this organization's login history");
        }
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "admin";
    }
}
