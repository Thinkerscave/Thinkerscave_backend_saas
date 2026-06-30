package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.CreateMenuRequest;
import com.thinkerscave.access.dto.request.UpdateMenuRequest;
import com.thinkerscave.access.dto.response.MenuResponse;
import com.thinkerscave.access.dto.response.SidebarItemResponse;
import com.thinkerscave.access.enums.MenuType;
import com.thinkerscave.access.service.MenuService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access/menus")
@RequiredArgsConstructor
@Tag(name = "Menu Management", description = "Menu/page tree and sidebar configuration")
public class MenuController {

    private final MenuService menuService;

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
    public ResponseEntity<ApiResponse<MenuResponse>> getMenu(@PathVariable Long menuId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenuById(menuId)));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get full menu tree (all menus hierarchically)")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getTree() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenuTree()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search menus with pagination")
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
    public ResponseEntity<ApiResponse<List<SidebarItemResponse>>> getSidebar(
            @RequestParam Long userId,
            @RequestParam Long organizationId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.buildSidebar(userId, organizationId)));
    }
}
