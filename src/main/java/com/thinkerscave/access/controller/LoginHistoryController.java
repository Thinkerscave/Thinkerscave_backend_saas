package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.response.LoginHistoryResponse;
import com.thinkerscave.access.enums.LoginStatus;
import com.thinkerscave.access.service.LoginHistoryService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/access/login-history")
@RequiredArgsConstructor
@Tag(name = "Login History", description = "Recent sign-ins. Events older than 30 days are not kept.")
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get login history for a specific user")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Page<LoginHistoryResponse>>> getUserHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) LoginStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("loginTime").descending());
        return ResponseEntity.ok(ApiResponse.success(loginHistoryService.getUserLoginHistory(
                userId,
                status,
                from == null ? null : LocalDateTime.ofInstant(from, ZoneId.systemDefault()),
                to == null ? null : LocalDateTime.ofInstant(to, ZoneId.systemDefault()),
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("loginTime").descending());
        return ResponseEntity.ok(ApiResponse.success(
                loginHistoryService.getOrganizationLoginHistory(
                        organizationId,
                        status,
                        from == null ? null : LocalDateTime.ofInstant(from, ZoneId.systemDefault()),
                        to == null ? null : LocalDateTime.ofInstant(to, ZoneId.systemDefault()),
                        pageable)));
    }
}
