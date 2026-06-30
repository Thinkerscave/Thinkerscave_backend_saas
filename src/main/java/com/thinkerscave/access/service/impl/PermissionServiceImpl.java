package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.UpdateUserPermissionsRequest;
import com.thinkerscave.access.dto.response.EffectivePermissionResponse;
import com.thinkerscave.access.entity.*;
import com.thinkerscave.access.repository.*;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final MenuRepository menuRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EffectivePermissionResponse> getEffectivePermissions(Long userId, Long organizationId) {
        // 1. Collect all role permissions for this user's active roles
        List<UserRole> activeRoles = userRoleRepository.findActiveRolesWithDetails(userId);
        Map<Long, EffectivePermissionResponse> merged = new LinkedHashMap<>();

        for (UserRole ur : activeRoles) {
            List<RolePermission> rolePerms = rolePermissionRepository
                    .findByRole_IdAndOrganization_Id(ur.getRole().getId(), organizationId);
            for (RolePermission rp : rolePerms) {
                merged.merge(rp.getMenu().getId(),
                        buildEffective(rp.getMenu(), rp.getCanView(), rp.getCanManage(), rp.getCanApprove(), false),
                        (existing, incoming) -> EffectivePermissionResponse.builder()
                                .menuId(existing.getMenuId())
                                .menuCode(existing.getMenuCode())
                                .menuName(existing.getMenuName())
                                .canView(Boolean.TRUE.equals(existing.getCanView()) || Boolean.TRUE.equals(incoming.getCanView()))
                                .canManage(Boolean.TRUE.equals(existing.getCanManage()) || Boolean.TRUE.equals(incoming.getCanManage()))
                                .canApprove(Boolean.TRUE.equals(existing.getCanApprove()) || Boolean.TRUE.equals(incoming.getCanApprove()))
                                .isOverride(false)
                                .build());
            }
        }

        // 2. Apply user-level overrides
        List<UserPermission> overrides = userPermissionRepository.findActiveWithMenu(userId);
        for (UserPermission up : overrides) {
            Long menuId = up.getMenu().getId();
            merged.put(menuId, buildEffective(up.getMenu(), up.getCanView(), up.getCanManage(), up.getCanApprove(), true));
        }

        return new ArrayList<>(merged.values());
    }

    @Override
    @Transactional
    public void updateUserPermissions(Long userId, Long organizationId, UpdateUserPermissionsRequest request) {
        userPermissionRepository.deleteAllByUser(userId);

        User userRef = new User();
        userRef.setId(userId);

        List<UserPermission> overrides = request.getOverrides().stream()
                .map(ov -> {
                    Menu menu = menuRepository.findById(ov.getMenuId())
                            .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + ov.getMenuId()));
                    User u = new User();
                    u.setId(userId);
                    return UserPermission.builder()
                            .user(u)
                            .menu(menu)
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
        return EffectivePermissionResponse.builder()
                .menuId(menu.getId())
                .menuCode(menu.getMenuCode())
                .menuName(menu.getMenuName())
                .canView(Boolean.TRUE.equals(view))
                .canManage(Boolean.TRUE.equals(manage))
                .canApprove(Boolean.TRUE.equals(approve))
                .isOverride(isOverride)
                .build();
    }
}
