package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.SecurityPolicyRequest;
import com.thinkerscave.access.dto.response.SecurityPolicyResponse;
import com.thinkerscave.access.service.SecurityPolicyService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/access/organizations/{organizationId}/security-policy")
@RequiredArgsConstructor
@Tag(name = "Security Policy", description = "Organization password and session security settings")
public class SecurityPolicyController {

    private final SecurityPolicyService securityPolicyService;

    @GetMapping
    @Operation(summary = "Get security policy for an organization")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SecurityPolicyResponse>> getPolicy(@PathVariable Long organizationId) {
        return ResponseEntity.ok(ApiResponse.success(securityPolicyService.getPolicy(organizationId)));
    }

    @PutMapping
    @Operation(summary = "Create or update security policy")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<SecurityPolicyResponse>> upsertPolicy(
            @PathVariable Long organizationId,
            @Valid @RequestBody SecurityPolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Security policy updated",
                securityPolicyService.createOrUpdatePolicy(organizationId, request)));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset security policy to system defaults")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> reset(@PathVariable Long organizationId) {
        securityPolicyService.resetToDefaults(organizationId);
        return ResponseEntity.ok(ApiResponse.noContent("Security policy reset to defaults"));
    }
}
