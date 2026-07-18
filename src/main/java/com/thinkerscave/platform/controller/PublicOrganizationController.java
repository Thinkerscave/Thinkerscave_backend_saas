package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.response.PublicOrganizationOptionResponse;
import com.thinkerscave.platform.service.OrganizationService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Unauthenticated organization directory used by the workspace org-select page.
 * Covered by SecurityConfig permitAll for {@code /api/v1/public/**}.
 */
@RestController
@RequestMapping("/api/v1/public/organizations")
@RequiredArgsConstructor
@Tag(name = "Public Organizations")
public class PublicOrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @Operation(summary = "List active institutions available for login selection")
    public ResponseEntity<ApiResponse<List<PublicOrganizationOptionResponse>>> list(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(
                "Organizations loaded",
                organizationService.listPublicOrganizations(search)));
    }
}
