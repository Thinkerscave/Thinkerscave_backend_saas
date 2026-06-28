package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.FeatureOverrideRequest;
import com.thinkerscave.platform.dto.request.OrganizationPromotionRequest;
import com.thinkerscave.platform.dto.request.OrganizationSubscriptionRequest;
import com.thinkerscave.platform.dto.request.SubscriptionChangeRequest;
import com.thinkerscave.platform.dto.response.FeatureOverrideResponse;
import com.thinkerscave.platform.dto.response.OrganizationPromotionResponse;
import com.thinkerscave.platform.dto.response.OrganizationSubscriptionResponse;
import com.thinkerscave.platform.enums.SubscriptionStatus;
import com.thinkerscave.platform.service.OrganizationSubscriptionService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Organization Subscription Management", description = "Manage organization subscriptions, promotions and feature overrides")
public class OrganizationSubscriptionController {

    private final OrganizationSubscriptionService subscriptionService;

    // ── Subscriptions ─────────────────────────────────────────────────────────

    @GetMapping("/api/platform/organization-subscriptions")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List subscriptions with pagination")
    public ResponseEntity<ApiResponse<Page<OrganizationSubscriptionResponse>>> getSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Subscriptions retrieved",
                subscriptionService.getSubscriptions(status, search, pageable)));
    }

    @GetMapping("/api/platform/organization-subscriptions/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get subscription detail")
    public ResponseEntity<ApiResponse<OrganizationSubscriptionResponse>> getSubscriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Subscription retrieved", subscriptionService.getSubscriptionById(id)));
    }

    @PostMapping("/api/platform/organization-subscriptions")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Create an organization subscription")
    public ResponseEntity<ApiResponse<OrganizationSubscriptionResponse>> createSubscription(
            @Valid @RequestBody OrganizationSubscriptionRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Subscription created", subscriptionService.createSubscription(request)));
    }

    @PutMapping("/api/platform/organization-subscriptions/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update a subscription")
    public ResponseEntity<ApiResponse<OrganizationSubscriptionResponse>> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationSubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subscription updated", subscriptionService.updateSubscription(id, request)));
    }

    @PostMapping("/api/platform/organization-subscriptions/{id}/renew")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Renew subscription")
    public ResponseEntity<ApiResponse<OrganizationSubscriptionResponse>> renewSubscription(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subscription renewed", subscriptionService.renewSubscription(id, request)));
    }

    @PostMapping("/api/platform/organization-subscriptions/{id}/upgrade")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Upgrade subscription plan")
    public ResponseEntity<ApiResponse<OrganizationSubscriptionResponse>> upgradeSubscription(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subscription upgraded", subscriptionService.upgradeSubscription(id, request)));
    }

    @PostMapping("/api/platform/organization-subscriptions/{id}/downgrade")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Downgrade subscription plan")
    public ResponseEntity<ApiResponse<OrganizationSubscriptionResponse>> downgradeSubscription(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subscription downgraded", subscriptionService.downgradeSubscription(id, request)));
    }

    @PostMapping("/api/platform/organization-subscriptions/{id}/cancel")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Cancel subscription")
    public ResponseEntity<ApiResponse<Void>> cancelSubscription(@PathVariable Long id) {
        subscriptionService.cancelSubscription(id);
        return ResponseEntity.ok(ApiResponse.noContent("Subscription cancelled"));
    }

    // ── Organization Promotions ───────────────────────────────────────────────

    @PostMapping("/api/platform/organization-promotions")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Apply a promotion to an organization")
    public ResponseEntity<ApiResponse<OrganizationPromotionResponse>> applyPromotion(
            @Valid @RequestBody OrganizationPromotionRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Promotion applied", subscriptionService.applyPromotion(request)));
    }

    @DeleteMapping("/api/platform/organization-promotions/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Remove applied promotion from organization")
    public ResponseEntity<ApiResponse<Void>> removePromotion(@PathVariable Long id) {
        subscriptionService.removePromotion(id);
        return ResponseEntity.ok(ApiResponse.noContent("Promotion removed"));
    }

    // ── Feature Overrides ─────────────────────────────────────────────────────

    @GetMapping("/api/platform/organization-subscriptions/{id}/feature-overrides")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List feature overrides for a subscription")
    public ResponseEntity<ApiResponse<List<FeatureOverrideResponse>>> getFeatureOverrides(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Feature overrides retrieved", subscriptionService.getFeatureOverrides(id)));
    }

    @PostMapping("/api/platform/feature-overrides")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Create a feature override")
    public ResponseEntity<ApiResponse<FeatureOverrideResponse>> createFeatureOverride(
            @Valid @RequestBody FeatureOverrideRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Feature override created", subscriptionService.createFeatureOverride(request)));
    }

    @PutMapping("/api/platform/feature-overrides/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update a feature override")
    public ResponseEntity<ApiResponse<FeatureOverrideResponse>> updateFeatureOverride(
            @PathVariable Long id,
            @Valid @RequestBody FeatureOverrideRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Feature override updated", subscriptionService.updateFeatureOverride(id, request)));
    }

    @DeleteMapping("/api/platform/feature-overrides/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete a feature override")
    public ResponseEntity<ApiResponse<Void>> deleteFeatureOverride(@PathVariable Long id) {
        subscriptionService.deleteFeatureOverride(id);
        return ResponseEntity.ok(ApiResponse.noContent("Feature override deleted"));
    }
}
