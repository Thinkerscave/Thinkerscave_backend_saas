package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.response.SubscriptionPlanResponse;
import com.thinkerscave.platform.service.SubscriptionPlanService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public catalog of visible subscription plans for the marketing landing page.
 * Covered by SecurityConfig permitAll for {@code /api/v1/public/**}.
 */
@RestController
@RequestMapping("/api/v1/public/subscription-plans")
@RequiredArgsConstructor
@Tag(name = "Public Subscription Plans")
public class PublicSubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @GetMapping
    @Operation(summary = "List active, publicly visible subscription plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> listVisiblePlans() {
        return ResponseEntity.ok(ApiResponse.success(
                "Subscription plans loaded",
                subscriptionPlanService.getVisiblePublicPlans()));
    }
}
