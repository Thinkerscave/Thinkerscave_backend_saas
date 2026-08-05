package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.CreateMenuRequest;
import com.thinkerscave.access.dto.request.UpdateMenuRequest;
import com.thinkerscave.access.dto.response.MenuResponse;
import com.thinkerscave.access.dto.response.SidebarItemResponse;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.enums.MenuType;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.MenuService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access/menus")
@RequiredArgsConstructor
@Tag(name = "Menu Management", description = "Menu/page tree and sidebar configuration")
public class MenuController {

    private final MenuService menuService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Create a new menu or page")
    @PreAuthorize("hasAuthority('ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<MenuResponse>> createMenu(@Valid @RequestBody CreateMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Menu created", menuService.createMenu(request)));
    }

    @PutMapping("/{menuId}")
    @Operation(summary = "Update menu")
    @PreAuthorize("hasAuthority('ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenu(
            @PathVariable Long menuId,
            @Valid @RequestBody UpdateMenuRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Menu updated", menuService.updateMenu(menuId, request)));
    }

    @GetMapping("/{menuId}")
    @Operation(summary = "Get menu by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MenuResponse>> getMenu(@PathVariable Long menuId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenuById(menuId)));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get full menu tree (all menus hierarchically)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getTree() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenuTree()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search menus with pagination")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<MenuResponse>>> searchMenus(
            @RequestParam(required = false) MenuType menuType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("displayOrder").ascending());
        return ResponseEntity.ok(ApiResponse.success(menuService.searchMenus(menuType, active, search, pageable)));
    }

    @PatchMapping("/{menuId}/activate")
    @Operation(summary = "Activate menu")
    @PreAuthorize("hasAuthority('ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long menuId) {
        menuService.activateMenu(menuId);
        return ResponseEntity.ok(ApiResponse.noContent("Menu activated"));
    }

    @PatchMapping("/{menuId}/deactivate")
    @Operation(summary = "Deactivate menu")
    @PreAuthorize("hasAuthority('ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long menuId) {
        menuService.deactivateMenu(menuId);
        return ResponseEntity.ok(ApiResponse.noContent("Menu deactivated"));
    }

    @GetMapping("/sidebar")
    @Operation(summary = "Build sidebar tree for a user with effective permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SidebarItemResponse>>> getSidebar(
            @RequestParam Long userId,
            @RequestParam Long organizationId) {
        assertCanViewSidebar(userId, organizationId);
        return ResponseEntity.ok(ApiResponse.success(menuService.buildSidebar(userId, organizationId)));
    }

    /**
     * IDOR guard: a caller may only request their own sidebar (own user id + own
     * organization id) unless they hold an org-admin-level authority, in which case
     * they may still only view users within their own organization.
     */
    private void assertCanViewSidebar(Long requestedUserId, Long requestedOrganizationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        User caller = userRepository.findByUsername(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new AccessDeniedException("Caller not resolvable"));

        boolean isSelf = caller.getId().equals(requestedUserId)
                && caller.getOrganizationId() != null
                && caller.getOrganizationId().equals(requestedOrganizationId);
        boolean isOrgAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ORGANIZATION_ADMIN") || a.getAuthority().equals("ORGANIZATION_OWNER")
                        || a.getAuthority().equals("SUPER_ADMIN"));

        if (isSelf) {
            return;
        }
        if (isOrgAdmin && caller.getOrganizationId() != null && caller.getOrganizationId().equals(requestedOrganizationId)) {
            return;
        }
        throw new AccessDeniedException("Not authorized to view this user's sidebar");
    }
}
