package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.*;
import com.thinkerscave.access.dto.response.*;
import com.thinkerscave.access.entity.*;
import com.thinkerscave.access.enums.MenuScope;
import com.thinkerscave.access.mapper.RoleMapper;
import com.thinkerscave.access.mapper.UserMapper;
import com.thinkerscave.access.repository.*;
import com.thinkerscave.access.service.RoleService;
import com.thinkerscave.access.specification.MenuSpecification;
import com.thinkerscave.shared.exceptions.*;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.service.TenantCatalogSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final MenuRepository menuRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationModuleRepository organizationModuleRepository;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserRoleRepository userRoleRepository;
    private final TenantCatalogSyncService tenantCatalogSyncService;

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByRoleCode(request.getRoleCode())) {
            throw new AlreadyExistsException("Role code already exists: " + request.getRoleCode());
        }
        Role role = Role.builder()
                .roleCode(request.getRoleCode())
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .roleType(request.getRoleType())
                .dashboardCode(request.getDashboardCode())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 1)
                .systemRole(true)
                .active(request.getActive() == null || Boolean.TRUE.equals(request.getActive()))
                .build();
        Role saved = roleRepository.save(role);
        tenantCatalogSyncService.syncRole(saved);
        return roleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long roleId, UpdateRoleRequest request) {
        Role role = findRoleById(roleId);
        if (StringUtils.hasText(request.getRoleName())
                && roleRepository.existsByRoleNameAndIdNot(request.getRoleName(), roleId)) {
            throw new AlreadyExistsException("Role name already in use: " + request.getRoleName());
        }
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setDashboardCode(request.getDashboardCode());
        if (request.getDisplayOrder() != null) {
            role.setDisplayOrder(request.getDisplayOrder());
        }
        Role saved = roleRepository.save(role);
        tenantCatalogSyncService.syncRole(saved);
        return roleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long roleId) {
        Role role = findRoleById(roleId);
        RoleResponse response = roleMapper.toResponse(role);
        response.setActiveUserCount(userRoleRepository.countActiveUsersByRole(roleId));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllActiveRoles() {
        List<RoleResponse> responses = roleMapper.toResponseList(roleRepository.findByActiveTrueOrderByDisplayOrderAsc());
        responses.forEach(res -> res.setActiveUserCount(userRoleRepository.countActiveUsersByRole(res.getId())));
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        List<RoleResponse> responses = roleMapper.toResponseList(roleRepository.findAllByOrderByDisplayOrderAsc());
        responses.forEach(res -> res.setActiveUserCount(userRoleRepository.countActiveUsersByRole(res.getId())));
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> searchRoles(Boolean active, String search, Pageable pageable) {
        Specification<Role> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("roleCode")), pattern),
                        cb.like(cb.lower(root.get("roleName")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return roleRepository.findAll(spec, pageable).map(r -> {
            RoleResponse res = roleMapper.toResponse(r);
            res.setActiveUserCount(userRoleRepository.countActiveUsersByRole(r.getId()));
            return res;
        });
    }

    @Override
    @Transactional
    public void activateRole(Long roleId) {
        Role role = findRoleById(roleId);
        role.setActive(true);
        tenantCatalogSyncService.syncRole(roleRepository.save(role));
        log.info("Role activated: {}", roleId);
    }

    @Override
    @Transactional
    public void deactivateRole(Long roleId) {
        Role role = findRoleById(roleId);
        if (Boolean.TRUE.equals(role.getSystemRole()) && role.getRoleType() != null
                && role.getRoleType().name().equals("SUPER_ADMIN")) {
            throw new ConflictException("Cannot deactivate the Super Admin role");
        }
        role.setActive(false);
        tenantCatalogSyncService.syncRole(roleRepository.save(role));
        log.info("Role deactivated: {}", roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getRoleUsers(Long roleId, Long organizationId) {
        findRoleById(roleId);
        return userRoleRepository.findActiveAssignmentsByRoleId(roleId).stream()
                .map(UserRole::getUser)
                .filter(user -> organizationId == null || organizationId.equals(user.getOrganizationId()))
                .map(userMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionMatrixResponse getPermissionMatrix(Long roleId, Long organizationId) {
        Role role = findRoleById(roleId);
        Organization org = findOrganization(organizationId);

        List<Long> enabledMenuIds = organizationModuleRepository.findEnabledMenuIds(organizationId);
        List<Menu> allMenus = menuRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .filter(menu -> menu.getMenuScope() == MenuScope.PLATFORM || enabledMenuIds.contains(menu.getId()))
                .toList();
        List<RolePermission> assigned = rolePermissionRepository.findByRole_IdAndOrganization_Id(roleId, organizationId);

        List<PermissionMatrixResponse.PermissionRow> rows = allMenus.stream().map(menu -> {
            RolePermission perm = assigned.stream()
                    .filter(p -> p.getMenu().getId().equals(menu.getId()))
                    .findFirst()
                    .orElse(null);
            return PermissionMatrixResponse.PermissionRow.builder()
                    .menuId(menu.getId())
                    .menuCode(menu.getMenuCode())
                    .menuName(menu.getMenuName())
                    .menuType(menu.getMenuType().name())
                    .parentMenuId(menu.getParentMenu() != null ? menu.getParentMenu().getId() : null)
                    .parentMenuName(menu.getParentMenu() != null ? menu.getParentMenu().getMenuName() : null)
                    .canView(perm != null && Boolean.TRUE.equals(perm.getCanView()))
                    .canManage(perm != null && Boolean.TRUE.equals(perm.getCanManage()))
                    .canApprove(perm != null && Boolean.TRUE.equals(perm.getCanApprove()))
                    .build();
        }).toList();

        return PermissionMatrixResponse.builder()
                .roleId(roleId)
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .organizationId(organizationId)
                .rows(rows)
                .build();
    }

    @Override
    @Transactional
    public void updatePermissionMatrix(Long roleId, Long organizationId, UpdateRolePermissionsRequest request) {
        findRoleById(roleId);
        Organization org = findOrganization(organizationId);

        // Delete all existing permissions for this role+org, then re-create
        rolePermissionRepository.deleteAllByRoleAndOrganization(roleId, organizationId);

        Role role = findRoleById(roleId);
        List<RolePermission> newPerms = request.getPermissions().stream()
                .filter(row -> Boolean.TRUE.equals(row.getCanView())
                        || Boolean.TRUE.equals(row.getCanManage())
                        || Boolean.TRUE.equals(row.getCanApprove()))
                .map(row -> {
                    Menu menu = menuRepository.findById(row.getMenuId())
                            .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + row.getMenuId()));
                    return RolePermission.builder()
                            .organization(org)
                            .role(role)
                            .menu(menu)
                            .canView(Boolean.TRUE.equals(row.getCanView()))
                            .canManage(Boolean.TRUE.equals(row.getCanManage()))
                            .canApprove(Boolean.TRUE.equals(row.getCanApprove()))
                            .build();
                }).toList();

        rolePermissionRepository.saveAll(newPerms);
        log.info("Permission matrix updated for role={} org={} rows={}", roleId, organizationId, newPerms.size());
    }

    private Role findRoleById(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));
    }

    private Organization findOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
    }
}
