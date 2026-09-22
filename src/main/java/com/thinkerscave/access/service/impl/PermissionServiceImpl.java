package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.UpdateUserPermissionsRequest;
import com.thinkerscave.access.dto.response.EffectivePermissionResponse;
import com.thinkerscave.access.entity.*;
import com.thinkerscave.access.enums.MenuScope;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.repository.*;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.ResponsibilityAssignment;
import com.thinkerscave.staff.repository.ResponsibilityAssignmentRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final MenuRepository menuRepository;
    private final OrganizationModuleRepository organizationModuleRepository;
    private final StaffRepository staffRepository;
    private final ResponsibilityAssignmentRepository responsibilityAssignmentRepository;
    private final ResponsibilityPermissionRepository responsibilityPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EffectivePermissionResponse> getEffectivePermissions(Long userId, Long organizationId) {
        List<UserRole> activeRoles = userRoleRepository.findActiveRolesWithDetails(userId);
        boolean isSuperAdmin = activeRoles.stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.getRoleType() == RoleType.SUPER_ADMIN);

        Map<Long, Menu> entitledMenus = resolveEntitledMenus(organizationId, isSuperAdmin);
        if (entitledMenus.isEmpty()) {
            return List.of();
        }

        Set<Long> entitledMenuIds = entitledMenus.keySet();
        Map<Long, EffectivePermissionResponse> merged = new LinkedHashMap<>();

        // 1. Role permissions are the baseline for all active roles.
        for (UserRole ur : activeRoles) {
            mergeRolePermissions(merged, ur.getRole().getId(), organizationId, entitledMenuIds);
        }

        // SUPER_ADMIN must always retain platform admin menus even if role matrix
        // rows were trimmed during tenant-focused role assignment operations.
        if (isSuperAdmin) {
            mergeSuperAdminPlatformMenus(merged, entitledMenus.values());
        }

        // 2. Responsibilities add to role baseline (primarily used by staff users).
        mergeResponsibilityPermissions(merged, userId, organizationId, entitledMenuIds);

        // 3. User-level overrides are authoritative and applied last, but still
        // constrained by organization feature entitlement.
        List<UserPermission> overrides = userPermissionRepository.findActiveWithMenu(userId, organizationId);
        for (UserPermission up : overrides) {
            Long menuId = up.getMenu().getId();
            if (!entitledMenuIds.contains(menuId)) {
                continue;
            }
            merged.put(menuId, buildEffective(up.getMenu(), up.getCanView(), up.getCanManage(), up.getCanApprove(), true));
        }

        ensureParentVisibility(merged, entitledMenus);

        return new ArrayList<>(merged.values());
    }

    private void mergeRolePermissions(Map<Long, EffectivePermissionResponse> merged,
                                      Long roleId,
                                      Long organizationId,
                                      Set<Long> entitledMenuIds) {
        List<RolePermission> rolePerms = rolePermissionRepository
                .findByRole_IdAndOrganization_Id(roleId, organizationId);
        for (RolePermission rp : rolePerms) {
            if (!entitledMenuIds.contains(rp.getMenu().getId())) {
                continue;
            }
            mergeOr(merged, rp.getMenu(), rp.getCanView(), rp.getCanManage(), rp.getCanApprove());
        }
    }

    private void mergeResponsibilityPermissions(Map<Long, EffectivePermissionResponse> merged,
                                                Long userId,
                                                Long organizationId,
                                                Set<Long> entitledMenuIds) {
        staffRepository.findByUser_Id(userId).ifPresent(staff -> {
            LocalDate today = LocalDate.now();
            List<ResponsibilityAssignment> assignments = responsibilityAssignmentRepository
                    .findByStaff_StaffIdAndActiveTrueOrderByEffectiveFromDesc(staff.getStaffId());
            for (ResponsibilityAssignment assignment : assignments) {
                if (assignment.getEffectiveFrom() != null && assignment.getEffectiveFrom().isAfter(today)) {
                    continue;
                }
                if (assignment.getEffectiveTo() != null && assignment.getEffectiveTo().isBefore(today)) {
                    continue;
                }
                Long responsibilityId = assignment.getResponsibility().getResponsibilityId();
                List<ResponsibilityPermission> perms = responsibilityPermissionRepository
                        .findByResponsibility_ResponsibilityIdAndOrganization_Id(responsibilityId, organizationId);
                for (ResponsibilityPermission rp : perms) {
                    if (!entitledMenuIds.contains(rp.getMenu().getId())) {
                        continue;
                    }
                    mergeOr(merged, rp.getMenu(), rp.getCanView(), rp.getCanManage(), rp.getCanApprove());
                }
            }
        });
    }

    private void mergeOr(Map<Long, EffectivePermissionResponse> merged, Menu menu, Boolean view, Boolean manage, Boolean approve) {
        merged.merge(menu.getId(),
                buildEffective(menu, view, manage, approve, false),
                (existing, incoming) -> EffectivePermissionResponse.builder()
                        .menuId(existing.getMenuId())
                        .menuCode(existing.getMenuCode())
                        .menuName(existing.getMenuName())
                        .menuType(existing.getMenuType())
                        .parentMenuId(existing.getParentMenuId())
                        .parentMenuName(existing.getParentMenuName())
                        .canView(Boolean.TRUE.equals(existing.getCanView()) || Boolean.TRUE.equals(incoming.getCanView()))
                        .canManage(Boolean.TRUE.equals(existing.getCanManage()) || Boolean.TRUE.equals(incoming.getCanManage()))
                        .canApprove(Boolean.TRUE.equals(existing.getCanApprove()) || Boolean.TRUE.equals(incoming.getCanApprove()))
                        .isOverride(false)
                        .build());
    }

    private void ensureParentVisibility(Map<Long, EffectivePermissionResponse> merged, Map<Long, Menu> entitledMenus) {
        List<Long> grantedMenuIds = merged.values().stream()
                .filter(this::isGranted)
                .map(EffectivePermissionResponse::getMenuId)
                .toList();

        for (Long grantedMenuId : grantedMenuIds) {
            Menu cursor = entitledMenus.get(grantedMenuId);
            while (cursor != null && cursor.getParentMenu() != null) {
                Menu parent = entitledMenus.get(cursor.getParentMenu().getId());
                if (parent == null) {
                    break;
                }
                mergeOr(merged, parent, true, false, false);
                cursor = parent;
            }
        }
    }

    private boolean isGranted(EffectivePermissionResponse row) {
        return Boolean.TRUE.equals(row.getCanView())
                || Boolean.TRUE.equals(row.getCanManage())
                || Boolean.TRUE.equals(row.getCanApprove());
    }

    private Map<Long, Menu> resolveEntitledMenus(Long organizationId, boolean includePlatformMenus) {
        List<Menu> activeMenus = menuRepository.findByActiveTrueOrderByDisplayOrderAsc();
        if (activeMenus.isEmpty()) {
            return Map.of();
        }

        Map<Long, Menu> byId = activeMenus.stream()
                .collect(java.util.stream.Collectors.toMap(Menu::getId, menu -> menu, (left, right) -> left));
        Set<Long> enabledMenuIds = new HashSet<>(organizationModuleRepository.findEnabledMenuIds(organizationId));
        Set<Long> included = new HashSet<>();

        for (Menu menu : activeMenus) {
            MenuScope scope = menu.getMenuScope();
            if (scope == MenuScope.CORE || enabledMenuIds.contains(menu.getId())
                    || (includePlatformMenus && scope == MenuScope.PLATFORM)) {
                included.add(menu.getId());
            }
        }

        boolean expanded = true;
        while (expanded) {
            expanded = false;
            for (Menu menu : activeMenus) {
                if (included.contains(menu.getId())) {
                    continue;
                }
                Menu parent = menu.getParentMenu();
                if (parent != null && included.contains(parent.getId())) {
                    included.add(menu.getId());
                    expanded = true;
                }
            }
        }

        for (Long id : Set.copyOf(included)) {
            Menu walk = byId.get(id);
            while (walk != null && walk.getParentMenu() != null) {
                Menu parent = byId.get(walk.getParentMenu().getId());
                if (parent == null) {
                    break;
                }
                if (!includePlatformMenus && parent.getMenuScope() == MenuScope.PLATFORM) {
                    break;
                }
                included.add(parent.getId());
                walk = parent;
            }
        }

        return activeMenus.stream()
                .filter(menu -> included.contains(menu.getId()))
                .filter(menu -> includePlatformMenus || menu.getMenuScope() != MenuScope.PLATFORM)
                .collect(java.util.stream.Collectors.toMap(Menu::getId, menu -> menu, (left, right) -> left, LinkedHashMap::new));
    }

    private void mergeSuperAdminPlatformMenus(Map<Long, EffectivePermissionResponse> merged, Collection<Menu> menus) {
        for (Menu menu : menus) {
            if (menu.getMenuScope() != MenuScope.PLATFORM) {
                continue;
            }
            mergeOr(merged, menu, true, true, true);
        }
    }

    @Override
    @Transactional
    public void updateUserPermissions(Long userId, Long organizationId, UpdateUserPermissionsRequest request) {
        userPermissionRepository.deleteAllByUserAndOrganization(userId, organizationId);

        User userRef = new User();
        userRef.setId(userId);
        Organization organizationRef = new Organization();
        organizationRef.setId(organizationId);

        List<UserPermission> overrides = request.getOverrides().stream()
                .map(ov -> {
                    Menu menu = menuRepository.findById(ov.getMenuId())
                            .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + ov.getMenuId()));
                    User u = new User();
                    u.setId(userId);
                    return UserPermission.builder()
                            .user(u)
                            .menu(menu)
                            .organization(organizationRef)
                            .canView(Boolean.TRUE.equals(ov.getCanView()))
                            .canManage(Boolean.TRUE.equals(ov.getCanManage()))
                            .canApprove(Boolean.TRUE.equals(ov.getCanApprove()))
                            .active(Boolean.TRUE.equals(ov.getActive()))
                            .build();
                }).toList();

        userPermissionRepository.saveAll(overrides);
        log.info("User permissions updated for userId={} orgId={} count={}", userId, organizationId, overrides.size());
    }

    @Override
    @Transactional(readOnly = true)
    public EffectivePermissionResponse checkPermission(Long userId, Long organizationId, Long menuId) {
        return getEffectivePermissions(userId, organizationId).stream()
                .filter(p -> p.getMenuId().equals(menuId))
                .findFirst()
                .orElse(EffectivePermissionResponse.builder()
                        .menuId(menuId)
                        .canView(false)
                        .canManage(false)
                        .canApprove(false)
                        .isOverride(false)
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, Long organizationId, String menuCode, String privilege) {
        return getEffectivePermissions(userId, organizationId).stream()
                .filter(p -> menuCode.equals(p.getMenuCode()))
                .anyMatch(p -> switch (privilege.toUpperCase()) {
                    case "VIEW"    -> Boolean.TRUE.equals(p.getCanView());
                    case "MANAGE"  -> Boolean.TRUE.equals(p.getCanManage());
                    case "APPROVE" -> Boolean.TRUE.equals(p.getCanApprove());
                    default        -> false;
                });
    }

    private EffectivePermissionResponse buildEffective(Menu menu, Boolean view, Boolean manage, Boolean approve, boolean isOverride) {
        Menu parent = menu.getParentMenu();
        boolean canManage = Boolean.TRUE.equals(manage);
        boolean canApprove = Boolean.TRUE.equals(approve);
        return EffectivePermissionResponse.builder()
                .menuId(menu.getId())
                .menuCode(menu.getMenuCode())
                .menuName(menu.getMenuName())
                .menuType(menu.getMenuType() != null ? menu.getMenuType().name() : null)
                .parentMenuId(parent != null ? parent.getId() : null)
                .parentMenuName(parent != null ? parent.getMenuName() : null)
                .canView(Boolean.TRUE.equals(view) || canManage || canApprove)
                .canManage(canManage)
                .canApprove(canApprove)
                .isOverride(isOverride)
                .build();
    }
}
