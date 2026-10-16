package com.thinkerscave.settings.controller;

import com.thinkerscave.settings.ModuleSettingsService;
import com.thinkerscave.settings.dto.ModuleSettingsRequest;
import com.thinkerscave.settings.dto.ModuleSettingsResponse;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings/{moduleCode}")
@RequiredArgsConstructor
@Tag(name = "Module Settings", description = "Validated organization-scoped module settings")
@PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
public class ModuleSettingsController {
    private final ModuleSettingsService moduleSettingsService;

    @GetMapping
    @Operation(summary = "Get settings and definitions for an organization module")
    public ResponseEntity<ApiResponse<ModuleSettingsResponse>> getSettings(@PathVariable String moduleCode) {
        return ResponseEntity.ok(ApiResponse.success("Settings retrieved",
                moduleSettingsService.getSettings(moduleCode)));
    }

    @PutMapping
    @Operation(summary = "Save validated settings for an organization module")
    public ResponseEntity<ApiResponse<ModuleSettingsResponse>> saveSettings(
            @PathVariable String moduleCode, @Valid @RequestBody ModuleSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Settings saved successfully",
                moduleSettingsService.saveSettings(moduleCode, request)));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset editable module settings to their defaults")
    public ResponseEntity<ApiResponse<ModuleSettingsResponse>> resetSettings(@PathVariable String moduleCode) {
        return ResponseEntity.ok(ApiResponse.success("Settings reset to defaults",
                moduleSettingsService.resetSettings(moduleCode)));
    }
}