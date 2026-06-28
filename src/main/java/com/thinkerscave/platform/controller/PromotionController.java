package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.PromotionRequest;
import com.thinkerscave.platform.dto.response.PromotionResponse;
import com.thinkerscave.platform.service.PromotionService;
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
@RequestMapping("/api/platform/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotion Management", description = "Manage subscription promotions and discounts")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List all promotions")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getAllPromotions() {
        return ResponseEntity.ok(ApiResponse.success("Promotions retrieved", promotionService.getAllPromotions()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get promotion by ID")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Promotion retrieved", promotionService.getPromotionById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Create a new promotion")
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Promotion created", promotionService.createPromotion(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update a promotion")
    public ResponseEntity<ApiResponse<PromotionResponse>> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Promotion updated", promotionService.updatePromotion(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Archive a promotion")
    public ResponseEntity<ApiResponse<Void>> archivePromotion(@PathVariable Long id) {
        promotionService.archivePromotion(id);
        return ResponseEntity.ok(ApiResponse.noContent("Promotion archived"));
    }
}
