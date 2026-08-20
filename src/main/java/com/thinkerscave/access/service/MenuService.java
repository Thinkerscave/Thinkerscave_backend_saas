package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.request.CreateMenuRequest;
import com.thinkerscave.access.dto.request.UpdateMenuRequest;
import com.thinkerscave.access.dto.response.MenuResponse;
import com.thinkerscave.access.dto.response.SidebarItemResponse;
import com.thinkerscave.access.enums.MenuType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Menu and sidebar operations.
 */
public interface MenuService {

    MenuResponse createMenu(CreateMenuRequest request);

    MenuResponse updateMenu(Long menuId, UpdateMenuRequest request);

    MenuResponse getMenuById(Long menuId);

    List<MenuResponse> getMenuTree(boolean includeInactive);

    void deleteMenu(Long menuId);

    Page<MenuResponse> searchMenus(MenuType menuType, Boolean active, String search, Pageable pageable);

    void activateMenu(Long menuId);

    void deactivateMenu(Long menuId);

    /**
     * Build sidebar tree for a user based on effective permissions.
     */
    List<SidebarItemResponse> buildSidebar(Long userId, Long organizationId);
}
