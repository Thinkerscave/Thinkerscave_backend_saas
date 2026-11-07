package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.response.LocalMigrationRunResponse;
import com.thinkerscave.platform.dto.response.LocalMigrationStatusResponse;
import com.thinkerscave.platform.service.LocalMigrationService;
import com.thinkerscave.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local development migration workflow. Disabled by default and loopback-only; see
 * {@link LocalMigrationService}. Never used against the shared platform database.
 */
@RestController
@RequestMapping("/api/platform/migration-center/local")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class LocalMigrationController {
    private final LocalMigrationService localMigrationService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<LocalMigrationStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success("Local migration status retrieved",
                localMigrationService.status()));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<LocalMigrationRunResponse>> validate() {
        return ResponseEntity.ok(ApiResponse.success("Local migration validation completed",
                localMigrationService.validate()));
    }

    @PostMapping("/migrate")
    public ResponseEntity<ApiResponse<LocalMigrationRunResponse>> migrate() {
        return ResponseEntity.ok(ApiResponse.success("Local migration finished",
                localMigrationService.migrate()));
    }
}
