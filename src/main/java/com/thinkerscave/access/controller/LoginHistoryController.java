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

@RestController
@RequestMapping("/api/access/login-history")
@RequiredArgsConstructor
@Tag(name = "Login History", description = "Audit log of login attempts")
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get login history for a specific user")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Page<LoginHistoryResponse>>> getUserHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) LoginStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("loginTime").descending());
        return ResponseEntity.ok(ApiResponse.success(loginHistoryService.getUserLoginHistory(userId, status, pageable)));
    }

    @GetMapping("/organizations/{organizationId}")
    @Operation(summary = "Get login history for all users in an organization")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Page<LoginHistoryResponse>>> getOrgHistory(
            @PathVariable Long organizationId,
            @RequestParam(required = false) LoginStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("loginTime").descending());
        return ResponseEntity.ok(ApiResponse.success(loginHistoryService.getOrganizationLoginHistory(organizationId, status, pageable)));
    }
}
