package com.thinkerscave.onboarding.controller;

import com.thinkerscave.onboarding.dto.OnboardingChecklistItemResponse;
import com.thinkerscave.onboarding.service.OnboardingService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Organization Onboarding", description = "First-login onboarding checklist for newly provisioned organizations")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/checklist")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get organization setup checklist completion status")
    public ResponseEntity<ApiResponse<List<OnboardingChecklistItemResponse>>> checklist() {
        return ResponseEntity.ok(ApiResponse.success("Onboarding checklist loaded", onboardingService.getChecklist()));
    }
}
