package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.FeatureMenuMappingRequest;
import com.thinkerscave.platform.dto.request.FeatureRequest;
import com.thinkerscave.platform.dto.response.FeatureResponse;
import com.thinkerscave.access.dto.response.MenuResponse;
import com.thinkerscave.platform.service.FeatureService;
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
@RequestMapping("/api/platform/features")
@RequiredArgsConstructor
@Tag(name = "Feature Catalog", description = "Manage platform feature catalog")
public class FeatureController {

    private final FeatureService featureService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List all features")
    public ResponseEntity<ApiResponse<List<FeatureResponse>>> getAllFeatures() {
        return ResponseEntity.ok(ApiResponse.success("Features retrieved", featureService.getAllFeatures()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get feature by ID")
    public ResponseEntity<ApiResponse<FeatureResponse>> getFeatureById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Feature retrieved", featureService.getFeatureById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Create a new feature")
    public ResponseEntity<ApiResponse<FeatureResponse>> createFeature(@Valid @RequestBody FeatureRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Feature created successfully", featureService.createFeature(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update a feature")
    public ResponseEntity<ApiResponse<FeatureResponse>> updateFeature(
            @PathVariable Long id,
            @Valid @RequestBody FeatureRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Feature updated successfully", featureService.updateFeature(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Archive a feature")
    public ResponseEntity<ApiResponse<Void>> deleteFeature(@PathVariable Long id) {
        featureService.deleteFeature(id);
        return ResponseEntity.ok(ApiResponse.noContent("Feature archived successfully"));
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List menus mapped to a feature")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getFeatureMenus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Feature menus retrieved", featureService.getFeatureMenus(id)));
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Replace the menus mapped to a feature")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> replaceFeatureMenus(
            @PathVariable Long id,
            @Valid @RequestBody FeatureMenuMappingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Feature menus updated", featureService.replaceFeatureMenus(id, request)));
    }
}
