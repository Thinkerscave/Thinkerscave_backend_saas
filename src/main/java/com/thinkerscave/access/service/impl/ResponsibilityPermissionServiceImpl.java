package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.UpdateRolePermissionsRequest;
import com.thinkerscave.access.dto.response.PermissionMatrixResponse;
import com.thinkerscave.access.entity.Menu;
import com.thinkerscave.access.entity.ResponsibilityPermission;
import com.thinkerscave.access.repository.MenuRepository;
import com.thinkerscave.access.repository.ResponsibilityPermissionRepository;
import com.thinkerscave.access.service.MenuService;
import com.thinkerscave.access.service.ResponsibilityPermissionService;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Responsibility;
import com.thinkerscave.staff.repository.ResponsibilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponsibilityPermissionServiceImpl implements ResponsibilityPermissionService {

    private final ResponsibilityRepository responsibilityRepository;
    private final ResponsibilityPermissionRepository permissionRepository;
    private final MenuRepository menuRepository;
    private final OrganizationRepository organizationRepository;
    private final MenuService menuService;

    @Override
    @Transactional(readOnly = true)
    public PermissionMatrixResponse getPermissionMatrix(Long responsibilityId, Long organizationId) {
        Responsibility responsibility = findResponsibility(responsibilityId);
        findOrganization(organizationId);

        List<Menu> allMenus = menuService.findEntitledMenus(organizationId);

        Map<Long, ResponsibilityPermission> assigned = permissionRepository
                .findByResponsibility_ResponsibilityIdAndOrganization_Id(responsibilityId, organizationId)
                .stream()
                .collect(Collectors.toMap(p -> p.getMenu().getId(), Function.identity(), (a, b) -> a));

        List<PermissionMatrixResponse.PermissionRow> rows = allMenus.stream().map(menu -> {
            ResponsibilityPermission perm = assigned.get(menu.getId());
            return PermissionMatrixResponse.PermissionRow.builder()
                    .menuId(menu.getId())
                    .menuCode(menu.getMenuCode())
                    .menuName(menu.getMenuName())
                    .menuType(menu.getMenuType() != null ? menu.getMenuType().name() : null)
                    .parentMenuId(menu.getParentMenu() != null ? menu.getParentMenu().getId() : null)
                    .parentMenuName(menu.getParentMenu() != null ? menu.getParentMenu().getMenuName() : null)
                    .displayOrder(menu.getDisplayOrder())
                    .canView(perm != null && Boolean.TRUE.equals(perm.getCanView()))
                    .canManage(perm != null && Boolean.TRUE.equals(perm.getCanManage()))
                    .canApprove(perm != null && Boolean.TRUE.equals(perm.getCanApprove()))
                    .build();
        }).toList();

        return PermissionMatrixResponse.builder()
                .responsibilityId(responsibilityId)
                .responsibilityCode(responsibility.getResponsibilityCode())
                .responsibilityName(responsibility.getResponsibilityName())
                .organizationId(organizationId)
                .rows(rows)
                .build();
    }

    @Override
    @Transactional
    public void updatePermissionMatrix(Long responsibilityId, Long organizationId, UpdateRolePermissionsRequest request) {
        Responsibility responsibility = findResponsibility(responsibilityId);
        Organization org = findOrganization(organizationId);

        permissionRepository.deleteAllByResponsibilityAndOrganization(responsibilityId, organizationId);

        List<ResponsibilityPermission> newPerms = request.getPermissions().stream()
                .filter(row -> Boolean.TRUE.equals(row.getCanView())
                        || Boolean.TRUE.equals(row.getCanManage())
                        || Boolean.TRUE.equals(row.getCanApprove()))
                .map(row -> {
                    Menu menu = menuRepository.findById(row.getMenuId())
                            .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + row.getMenuId()));
                    return ResponsibilityPermission.builder()
                            .organization(org)
                            .responsibility(responsibility)
                            .menu(menu)
                            .canView(Boolean.TRUE.equals(row.getCanView()))
                            .canManage(Boolean.TRUE.equals(row.getCanManage()))
                            .canApprove(Boolean.TRUE.equals(row.getCanApprove()))
                            .build();
                }).toList();

        permissionRepository.saveAll(newPerms);
        log.info("Responsibility permission matrix updated: resp={} org={} rows={}",
                responsibilityId, organizationId, newPerms.size());
    }

    private Responsibility findResponsibility(Long responsibilityId) {
        return responsibilityRepository.findById(responsibilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsibility not found: " + responsibilityId));
    }

    private Organization findOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
    }
}
