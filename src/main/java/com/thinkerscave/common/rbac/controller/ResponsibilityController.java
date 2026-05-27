package com.thinkerscave.common.rbac.controller;

import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.rbac.dto.ResponsibilityDTO;
import com.thinkerscave.common.rbac.dto.UserResponsibilityDTO;
import com.thinkerscave.common.rbac.service.ResponsibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/responsibilities")
@Tag(name = "Responsibilities", description = "Responsibility CRUD + user assignment")
@RequiredArgsConstructor
@Slf4j
public class ResponsibilityController {

    private final ResponsibilityService responsibilityService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('RESPONSIBILITY_VIEW')")
    public ResponseEntity<ApiResponse<List<ResponsibilityDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(responsibilityService.list()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('RESPONSIBILITY_VIEW')")
    public ResponseEntity<ApiResponse<ResponsibilityDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(responsibilityService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('RESPONSIBILITY_EDIT')")
    public ResponseEntity<ApiResponse<ResponsibilityDTO>> save(
            @Valid @RequestBody ResponsibilityDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Responsibility created" : "Responsibility updated",
                responsibilityService.save(dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('RESPONSIBILITY_EDIT')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        responsibilityService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Responsibility deleted", null));
    }

    // ---- user assignments ----------------------------------------------------

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('RESPONSIBILITY_VIEW')")
    @Operation(summary = "Active responsibility assignments for a user")
    public ResponseEntity<ApiResponse<List<UserResponsibilityDTO>>> forUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(responsibilityService.listForUser(userId)));
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('RESPONSIBILITY_ASSIGN')")
    public ResponseEntity<ApiResponse<UserResponsibilityDTO>> assign(
            @Valid @RequestBody UserResponsibilityDTO dto) {
        return ResponseEntity.ok(ApiResponse.created("Responsibility assigned",
                responsibilityService.assign(dto)));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('RESPONSIBILITY_ASSIGN')")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable Long assignmentId) {
        responsibilityService.revoke(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Responsibility revoked", null));
    }
}
