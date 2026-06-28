package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.ProvisioningTemplateItemRequest;
import com.thinkerscave.platform.dto.request.ProvisioningTemplateRequest;
import com.thinkerscave.platform.dto.response.ProvisioningTemplateItemResponse;
import com.thinkerscave.platform.dto.response.ProvisioningTemplateResponse;
import com.thinkerscave.platform.service.ProvisioningTemplateService;
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
@Tag(name = "Provisioning Template Management", description = "Manage provisioning templates and their items")
public class ProvisioningTemplateController {

    private final ProvisioningTemplateService templateService;

    // ── Templates ─────────────────────────────────────────────────────────────

    @GetMapping("/api/platform/provisioning-templates")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List all provisioning templates")
    public ResponseEntity<ApiResponse<List<ProvisioningTemplateResponse>>> getAllTemplates() {
        return ResponseEntity.ok(ApiResponse.success("Provisioning templates retrieved", templateService.getAllTemplates()));
    }

    @GetMapping("/api/platform/provisioning-templates/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get provisioning template by ID")
    public ResponseEntity<ApiResponse<ProvisioningTemplateResponse>> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Template retrieved", templateService.getTemplateById(id)));
    }

    @PostMapping("/api/platform/provisioning-templates")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Create a provisioning template")
    public ResponseEntity<ApiResponse<ProvisioningTemplateResponse>> createTemplate(
            @Valid @RequestBody ProvisioningTemplateRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Template created", templateService.createTemplate(request)));
    }

    @PutMapping("/api/platform/provisioning-templates/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update a provisioning template")
    public ResponseEntity<ApiResponse<ProvisioningTemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ProvisioningTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Template updated", templateService.updateTemplate(id, request)));
    }

    @DeleteMapping("/api/platform/provisioning-templates/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Archive provisioning template")
    public ResponseEntity<ApiResponse<Void>> archiveTemplate(@PathVariable Long id) {
        templateService.archiveTemplate(id);
        return ResponseEntity.ok(ApiResponse.noContent("Template archived"));
    }

    // ── Template Items ────────────────────────────────────────────────────────

    @GetMapping("/api/platform/provisioning-templates/{id}/items")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List items of a provisioning template")
    public ResponseEntity<ApiResponse<List<ProvisioningTemplateItemResponse>>> getTemplateItems(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Template items retrieved", templateService.getTemplateItems(id)));
    }

    @PostMapping("/api/platform/provisioning-template-items")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Add item to provisioning template")
    public ResponseEntity<ApiResponse<ProvisioningTemplateItemResponse>> addTemplateItem(
            @Valid @RequestBody ProvisioningTemplateItemRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Template item added", templateService.addTemplateItem(request)));
    }

    @PutMapping("/api/platform/provisioning-template-items/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update provisioning template item")
    public ResponseEntity<ApiResponse<ProvisioningTemplateItemResponse>> updateTemplateItem(
            @PathVariable Long id,
            @Valid @RequestBody ProvisioningTemplateItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Template item updated", templateService.updateTemplateItem(id, request)));
    }

    @DeleteMapping("/api/platform/provisioning-template-items/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete provisioning template item")
    public ResponseEntity<ApiResponse<Void>> deleteTemplateItem(@PathVariable Long id) {
        templateService.deleteTemplateItem(id);
        return ResponseEntity.ok(ApiResponse.noContent("Template item deleted"));
    }
}
