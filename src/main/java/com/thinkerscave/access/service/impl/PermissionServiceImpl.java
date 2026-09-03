package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.UpdateUserPermissionsRequest;
import com.thinkerscave.access.dto.response.EffectivePermissionResponse;
import com.thinkerscave.access.entity.*;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.repository.*;
import com.thinkerscave.access.service.PermissionService;
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
    private final StaffRepository staffRepository;
    private final ResponsibilityAssignmentRepository responsibilityAssignmentRepository;
    private final ResponsibilityPermissionRepository responsibilityPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EffectivePermissionResponse> getEffectivePermissions(Long userId, Long organizationId) {
        Map<Long, EffectivePermissionResponse> merged = new LinkedHashMap<>();

        // 1. Admin/owner roles keep full role-based menu access; staff-type roles must
        // never grant menus by role alone — their access comes only from responsibilities
        // assigned via Access Management, so a staff user with no responsibility sees nothing.
        List<UserRole> activeRoles = userRoleRepository.findActiveRolesWithDetails(userId);
        for (UserRole ur : activeRoles) {
            RoleType roleType = ur.getRole().getRoleType();
            if (roleType == RoleType.SUPER_ADMIN || roleType == RoleType.ORGANIZATION_OWNER || roleType == RoleType.ORGANIZATION_ADMIN) {
                mergeRolePermissions(merged, ur.getRole().getId(), organizationId);
            }
        }

        // 2. Merge menus granted via the user's active responsibility assignments
        // (a user can hold multiple responsibilities; their menus are unioned/deduped).
        mergeResponsibilityPermissions(merged, userId, organizationId);

        // 3. Apply user-level overrides
        List<UserPermission> overrides = userPermissionRepository.findActiveWithMenu(userId);
        for (UserPermission up : overrides) {
            Long menuId = up.getMenu().getId();
            merged.put(menuId, buildEffective(up.getMenu(), up.getCanView(), up.getCanManage(), up.getCanApprove(), true));
        }

        return new ArrayList<>(merged.values());
    }

    private void mergeRolePermissions(Map<Long, EffectivePermissionResponse> merged, Long roleId, Long organizationId) {
        List<RolePermission> rolePerms = rolePermissionRepository
                .findByRole_IdAndOrganization_Id(roleId, organizationId);
        for (RolePermission rp : rolePerms) {
            mergeOr(merged, rp.getMenu(), rp.getCanView(), rp.getCanManage(), rp.getCanApprove());
        }
    }

    private void mergeResponsibilityPermissions(Map<Long, EffectivePermissionResponse> merged, Long userId, Long organizationId) {
        staffRepository.findByUser_Id(userId).ifPresent(staff -> {
            LocalDate today = LocalDate.now();
            List<ResponsibilityAssignment> assignments = responsibilityAssignmentRepository
                    .findByStaff_StaffIdAndActiveTrueOrderByEffectiveFromDesc(staff.getStaffId());
            for (ResponsibilityAssignment assignment : assignments) {
                if (assignment.getEffectiveTo() != null && assignment.getEffectiveTo().isBefore(today)) {
                    continue;
                }
                Long responsibilityId = assignment.getResponsibility().getResponsibilityId();
                List<ResponsibilityPermission> perms = responsibilityPermissionRepository
                        .findByResponsibility_ResponsibilityIdAndOrganization_Id(responsibilityId, organizationId);
                for (ResponsibilityPermission rp : perms) {
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
        Menu parent = menu.getParentMenu();
        return EffectivePermissionResponse.builder()
                .menuId(menu.getId())
                .menuCode(menu.getMenuCode())
                .menuName(menu.getMenuName())
                .menuType(menu.getMenuType() != null ? menu.getMenuType().name() : null)
                .parentMenuId(parent != null ? parent.getId() : null)
                .parentMenuName(parent != null ? parent.getMenuName() : null)
                .canView(Boolean.TRUE.equals(view))
                .canManage(Boolean.TRUE.equals(manage))
                .canApprove(Boolean.TRUE.equals(approve))
                .isOverride(isOverride)
                .build();
    }
}
