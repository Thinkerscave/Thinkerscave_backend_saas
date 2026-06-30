package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.response.SessionResponse;
import com.thinkerscave.access.service.AuthService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/access/sessions")
@RequiredArgsConstructor
@Tag(name = "Session Management", description = "Active session listing and termination")
public class SessionController {

    private final AuthService authService;

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get all sessions for a user")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Page<SessionResponse>>> getUserSessions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("loginAt").descending());
        return ResponseEntity.ok(ApiResponse.success(authService.getUserSessions(userId, pageable)));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Terminate a specific session")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> terminateSession(@PathVariable Long sessionId) {
        authService.terminateSession(sessionId);
        return ResponseEntity.ok(ApiResponse.noContent("Session terminated"));
    }

    @DeleteMapping("/users/{userId}/all")
    @Operation(summary = "Terminate all active sessions for a user")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@PathVariable Long userId) {
        authService.logoutAllSessions(userId);
        return ResponseEntity.ok(ApiResponse.noContent("All sessions terminated"));
    }
}
