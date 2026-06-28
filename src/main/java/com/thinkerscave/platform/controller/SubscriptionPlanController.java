package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.SubscriptionPlanFeatureRequest;
import com.thinkerscave.platform.dto.request.SubscriptionPlanRequest;
import com.thinkerscave.platform.dto.response.SubscriptionPlanFeatureResponse;
import com.thinkerscave.platform.dto.response.SubscriptionPlanResponse;
import com.thinkerscave.platform.service.SubscriptionPlanService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Subscription Plan Management", description = "Manage subscription plans and their features")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    // ── Plans ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/platform/subscription-plans")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List all subscription plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getAllPlans() {
        return ResponseEntity.ok(ApiResponse.success("Subscription plans retrieved", subscriptionPlanService.getAllPlans()));
    }

    @GetMapping("/api/platform/subscription-plans/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get subscription plan by ID")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Subscription plan retrieved", subscriptionPlanService.getPlanById(id)));
    }

    @PostMapping("/api/platform/subscription-plans")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Create a new subscription plan")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> createPlan(@Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Subscription plan created", subscriptionPlanService.createPlan(request)));
    }

    @PutMapping("/api/platform/subscription-plans/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update a subscription plan")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subscription plan updated", subscriptionPlanService.updatePlan(id, request)));
    }

    @DeleteMapping("/api/platform/subscription-plans/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Archive subscription plan")
    public ResponseEntity<ApiResponse<Void>> archivePlan(@PathVariable Long id) {
        subscriptionPlanService.archivePlan(id);
        return ResponseEntity.ok(ApiResponse.noContent("Subscription plan archived"));
    }

    // ── Plan Features ─────────────────────────────────────────────────────────

    @GetMapping("/api/platform/subscription-plans/{planId}/features")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List features of a subscription plan")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanFeatureResponse>>> getPlanFeatures(@PathVariable Long planId) {
        return ResponseEntity.ok(ApiResponse.success("Plan features retrieved", subscriptionPlanService.getPlanFeatures(planId)));
    }

    @PostMapping("/api/platform/subscription-plans/{planId}/features")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Add a feature to a subscription plan")
    public ResponseEntity<ApiResponse<SubscriptionPlanFeatureResponse>> addPlanFeature(
            @PathVariable Long planId,
            @Valid @RequestBody SubscriptionPlanFeatureRequest request) {
        request.setSubscriptionPlanId(planId);
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Feature added to plan", subscriptionPlanService.addPlanFeature(request)));
    }

    @PutMapping("/api/platform/subscription-plan-features/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update a subscription plan feature mapping")
    public ResponseEntity<ApiResponse<SubscriptionPlanFeatureResponse>> updatePlanFeature(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionPlanFeatureRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Plan feature updated", subscriptionPlanService.updatePlanFeature(id, request)));
    }

    @DeleteMapping("/api/platform/subscription-plan-features/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Remove a feature from a subscription plan")
    public ResponseEntity<ApiResponse<Void>> removePlanFeature(@PathVariable Long id) {
        subscriptionPlanService.removePlanFeature(id);
        return ResponseEntity.ok(ApiResponse.noContent("Feature removed from plan"));
    }
}
