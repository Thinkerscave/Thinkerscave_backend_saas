package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.CreateMenuRequest;
import com.thinkerscave.access.dto.request.UpdateMenuRequest;
import com.thinkerscave.access.dto.response.MenuResponse;
import com.thinkerscave.access.dto.response.SidebarItemResponse;
import com.thinkerscave.access.entity.Menu;
import com.thinkerscave.access.entity.UserRole;
import com.thinkerscave.access.enums.MenuType;
import com.thinkerscave.access.mapper.MenuMapper;
import com.thinkerscave.access.repository.MenuRepository;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.repository.UserRoleRepository;
import com.thinkerscave.access.service.MenuService;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.access.specification.MenuSpecification;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ConflictException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final UserRoleRepository userRoleRepository;
    private final PermissionService permissionService;

    @Override
    @Transactional
    public MenuResponse createMenu(CreateMenuRequest request) {
        if (menuRepository.existsByMenuCode(request.getMenuCode())) {
            throw new AlreadyExistsException("Menu code already exists: " + request.getMenuCode());
        }

        Menu parent = null;
        if (request.getParentMenuId() != null) {
            parent = menuRepository.findById(request.getParentMenuId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent menu not found: " + request.getParentMenuId()));
            if (parent.getMenuType() == MenuType.PAGE) {
                throw new BadRequestException("A PAGE cannot be a parent menu");
            }
        }

        Menu menu = Menu.builder()
                .menuCode(request.getMenuCode())
                .menuName(request.getMenuName())
                .description(request.getDescription())
                .route(request.getRoute())
                .icon(request.getIcon())
                .menuType(request.getMenuType())
                .parentMenu(parent)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 1)
                .showInSidebar(Boolean.TRUE.equals(request.getShowInSidebar()))
                .defaultPage(Boolean.TRUE.equals(request.getDefaultPage()))
                .active(true)
                .build();

        return menuMapper.toResponse(menuRepository.save(menu));
    }

    @Override
    @Transactional
    public MenuResponse updateMenu(Long menuId, UpdateMenuRequest request) {
        Menu menu = findMenuById(menuId);

        if (request.getParentMenuId() != null && !request.getParentMenuId().equals(
                menu.getParentMenu() != null ? menu.getParentMenu().getId() : null)) {
            // Validate no circular reference
            validateNoCircularHierarchy(menuId, request.getParentMenuId());
            Menu newParent = menuRepository.findById(request.getParentMenuId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent menu not found"));
            menu.setParentMenu(newParent);
        } else if (request.getParentMenuId() == null) {
            menu.setParentMenu(null);
        }

        menu.setMenuName(request.getMenuName());
        if (request.getDescription() != null) menu.setDescription(request.getDescription());
        if (request.getRoute() != null) menu.setRoute(request.getRoute());
        if (request.getIcon() != null) menu.setIcon(request.getIcon());
        if (request.getDisplayOrder() != null) menu.setDisplayOrder(request.getDisplayOrder());
        if (request.getShowInSidebar() != null) menu.setShowInSidebar(request.getShowInSidebar());
        if (request.getDefaultPage() != null) menu.setDefaultPage(request.getDefaultPage());

        return menuMapper.toResponse(menuRepository.save(menu));
    }

    @Override
    @Transactional(readOnly = true)
    public MenuResponse getMenuById(Long menuId) {
        return menuMapper.toResponse(findMenuById(menuId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenuTree() {
        List<Menu> allMenus = menuRepository.findByActiveTrueOrderByDisplayOrderAsc();
        return buildTree(allMenus);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MenuResponse> searchMenus(MenuType menuType, Boolean active, String search, Pageable pageable) {
        var spec = MenuSpecification.filter(menuType, active, null, search);
        return menuRepository.findAll(spec, pageable).map(menuMapper::toResponse);
    }

    @Override
    @Transactional
    public void activateMenu(Long menuId) {
        Menu menu = findMenuById(menuId);
        menu.setActive(true);
        menuRepository.save(menu);
    }

    @Override
    @Transactional
    public void deactivateMenu(Long menuId) {
        Menu menu = findMenuById(menuId);
        if (menuRepository.hasChildren(menuId)) {
            throw new ConflictException("Cannot deactivate a menu that has active child menus");
        }
        menu.setActive(false);
        menuRepository.save(menu);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SidebarItemResponse> buildSidebar(Long userId, Long organizationId) {
        var effectivePerms = permissionService.getEffectivePermissions(userId, organizationId);
        Map<Long, com.thinkerscave.access.dto.response.EffectivePermissionResponse> permMap =
                effectivePerms.stream()
                        .filter(p -> Boolean.TRUE.equals(p.getCanView()))
                        .collect(Collectors.toMap(
                                com.thinkerscave.access.dto.response.EffectivePermissionResponse::getMenuId,
                                p -> p));

        // Fetch all active menus in a single query and group children by parent id in-memory
        // to avoid an N+1 query pattern (one query per menu node) which caused the sidebar
        // endpoint to take 50+ seconds against a remote/high-latency database.
        List<Menu> allActiveMenus = menuRepository.findByActiveTrueOrderByDisplayOrderAsc();
        Map<Long, List<Menu>> childrenByParentId = allActiveMenus.stream()
                .filter(m -> m.getParentMenu() != null)
                .collect(Collectors.groupingBy(m -> m.getParentMenu().getId()));
        List<Menu> topLevelMenus = allActiveMenus.stream()
                .filter(m -> m.getParentMenu() == null)
                .toList();

        return buildSidebarTree(topLevelMenus, permMap, organizationId, childrenByParentId);
    }

    // ─── Tree Builders ────────────────────────────────────────────────────

    private List<MenuResponse> buildTree(List<Menu> all) {
        Map<Long, MenuResponse> map = all.stream()
                .collect(Collectors.toMap(Menu::getId, m -> {
                    MenuResponse r = menuMapper.toResponse(m);
                    r.setChildren(new ArrayList<>());
                    return r;
                }));

        List<MenuResponse> roots = new ArrayList<>();
        for (Menu m : all) {
            MenuResponse node = map.get(m.getId());
            if (m.getParentMenu() == null) {
                roots.add(node);
            } else {
                MenuResponse parent = map.get(m.getParentMenu().getId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        return roots;
    }

    private List<SidebarItemResponse> buildSidebarTree(List<Menu> parents,
            Map<Long, com.thinkerscave.access.dto.response.EffectivePermissionResponse> permMap,
            Long organizationId,
            Map<Long, List<Menu>> childrenByParentId) {
        List<SidebarItemResponse> result = new ArrayList<>();
        for (Menu parent : parents) {
            if (!Boolean.TRUE.equals(parent.getShowInSidebar())) continue;

            com.thinkerscave.access.dto.response.EffectivePermissionResponse perm = permMap.get(parent.getId());
            List<Menu> children = childrenByParentId.getOrDefault(parent.getId(), List.of());
            List<SidebarItemResponse> childNodes = buildSidebarTree(children, permMap, organizationId, childrenByParentId);

            boolean hasAccess = perm != null || !childNodes.isEmpty();
            if (!hasAccess) continue;

            result.add(SidebarItemResponse.builder()
                    .id(parent.getId())
                    .menuCode(parent.getMenuCode())
                    .menuName(parent.getMenuName())
                    .route(parent.getRoute())
                    .icon(parent.getIcon())
                    .displayOrder(parent.getDisplayOrder())
                    .defaultPage(parent.getDefaultPage())
                    .canView(perm != null ? perm.getCanView() : false)
                    .canManage(perm != null ? perm.getCanManage() : false)
                    .canApprove(perm != null ? perm.getCanApprove() : false)
                    .children(childNodes)
                    .build());
        }
        return result;
    }

    private void validateNoCircularHierarchy(Long menuId, Long newParentId) {
        if (menuId.equals(newParentId)) {
            throw new BadRequestException("A menu cannot be its own parent");
        }
        Menu current = menuRepository.findById(newParentId).orElse(null);
        while (current != null && current.getParentMenu() != null) {
            if (current.getParentMenu().getId().equals(menuId)) {
                throw new BadRequestException("Circular menu hierarchy detected");
            }
            current = current.getParentMenu();
        }
    }

    private Menu findMenuById(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + menuId));
    }
}
