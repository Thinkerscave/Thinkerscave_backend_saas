package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.CreateMenuRequest;
import com.thinkerscave.access.dto.request.UpdateMenuRequest;
import com.thinkerscave.access.dto.response.MenuResponse;
import com.thinkerscave.access.dto.response.SidebarItemResponse;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.enums.MenuType;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.MenuService;
import com.thinkerscave.security.service.impl.PublicSchemaUserLookupService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/access/menus")
@RequiredArgsConstructor
@Tag(name = "Menu Management", description = "Menu/page tree and sidebar configuration")
public class MenuController {

    private final MenuService menuService;
    private final UserRepository userRepository;
    private final PublicSchemaUserLookupService publicSchemaUserLookupService;

    @PostMapping
    @Operation(summary = "Create a new menu or page")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MenuResponse>> createMenu(@Valid @RequestBody CreateMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Menu created", menuService.createMenu(request)));
    }

    @PutMapping("/{menuId}")
    @Operation(summary = "Update menu")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
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
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getTree(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenuTree(includeInactive)));
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
    @Operation(summary = "Activate / save menu")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long menuId) {
        menuService.activateMenu(menuId);
        return ResponseEntity.ok(ApiResponse.noContent("Menu activated"));
    }

    @PatchMapping("/{menuId}/deactivate")
    @Operation(summary = "Deactivate menu")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long menuId) {
        menuService.deactivateMenu(menuId);
        return ResponseEntity.ok(ApiResponse.noContent("Menu deactivated"));
    }

    @DeleteMapping("/{menuId}")
    @Operation(summary = "Delete a draft or unused menu")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(@PathVariable Long menuId) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.ok(ApiResponse.noContent("Menu deleted"));
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
     *
     * <p>Organization Owners live in the public schema only — ambient tenant
     * {@link UserRepository} lookups miss them, so we fall back to a public-schema
     * lookup. Org scope is taken from the caller's row, {@link OrganizationContext},
     * or the request's {@code X-Organization-ID} header.
     */
    private void assertCanViewSidebar(Long requestedUserId, Long requestedOrganizationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        User caller = resolveCaller(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Caller not resolvable"));

        boolean isOrgAdmin = hasAnyAuthority(authentication,
                "ORGANIZATION_ADMIN", "ORGANIZATION_OWNER", "SUPER_ADMIN");
        boolean sameUser = caller.getId().equals(requestedUserId);
        boolean sameOrg = isOrganizationInScope(caller, requestedOrganizationId);

        if (sameUser && sameOrg) {
            return;
        }
        if (isOrgAdmin && sameOrg) {
            return;
        }
        throw new AccessDeniedException("Not authorized to view this user's sidebar");
    }

    private Optional<User> resolveCaller(String usernameOrEmail) {
        Optional<User> ambient = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));
        if (ambient.isPresent()) {
            return ambient;
        }
        String previousTenant = TenantContext.getTenant();
        try {
            TenantContext.setTenant("public");
            return publicSchemaUserLookupService.findAnyInPublicSchema(usernameOrEmail);
        } finally {
            TenantContext.setTenant(previousTenant);
        }
    }

    private boolean isOrganizationInScope(User caller, Long requestedOrganizationId) {
        if (requestedOrganizationId == null || requestedOrganizationId <= 0) {
            return false;
        }
        Long callerOrgId = caller.getOrganizationId();
        if (callerOrgId != null && callerOrgId > 0 && callerOrgId.equals(requestedOrganizationId)) {
            return true;
        }
        Long contextOrgId = OrganizationContext.getOrganizationId();
        if (contextOrgId != null && contextOrgId > 0 && contextOrgId.equals(requestedOrganizationId)) {
            return true;
        }
        Long headerOrgId = resolveOrganizationIdFromHeader();
        return headerOrgId != null && headerOrgId.equals(requestedOrganizationId);
    }

    private Long resolveOrganizationIdFromHeader() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String raw = request.getHeader("X-Organization-ID");
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(raw.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        Collection<? extends GrantedAuthority> granted = authentication.getAuthorities();
        for (String authority : authorities) {
            for (GrantedAuthority ga : granted) {
                if (authority.equals(ga.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }
}
