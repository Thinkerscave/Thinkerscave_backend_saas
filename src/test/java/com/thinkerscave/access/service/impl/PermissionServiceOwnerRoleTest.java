package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.Menu;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.entity.UserRole;
import com.thinkerscave.access.dto.response.EffectivePermissionResponse;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.enums.MenuType;
import com.thinkerscave.access.repository.*;
import com.thinkerscave.security.service.impl.PublicSchemaUserLookupService;
import com.thinkerscave.shared.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.thinkerscave.access.repository.ResponsibilityAssignmentRepository;
import com.thinkerscave.staff.repository.StaffRepository;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceOwnerRoleTest {

    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private UserPermissionRepository userPermissionRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private OrganizationModuleRepository organizationModuleRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private ResponsibilityAssignmentRepository responsibilityAssignmentRepository;
    @Mock private ResponsibilityPermissionRepository responsibilityPermissionRepository;
    @Mock private PublicSchemaUserLookupService publicSchemaUserLookupService;

    private PermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PermissionServiceImpl(
                userRoleRepository, rolePermissionRepository, userPermissionRepository, menuRepository,
                organizationModuleRepository, staffRepository, responsibilityAssignmentRepository,
                responsibilityPermissionRepository, publicSchemaUserLookupService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void mergesPublicOwnerRoleWhenTenantUserIdCollidesWithAnotherRole() {
        Long userId = 26L;
        Long organizationId = 268L;
        UserRole tenantAdminRole = role(userId, RoleType.ORGANIZATION_ADMIN, 2L);
        UserRole publicOwnerRole = role(userId, RoleType.ORGANIZATION_OWNER, 3L);
        User owner = new User();
        owner.setId(userId);
        owner.setUsername("owner@example.test");
        owner.setUserRoles(Set.of(publicOwnerRole));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner.getUsername(), null, List.of(new SimpleGrantedAuthority(RoleType.ORGANIZATION_OWNER.name()))));
        TenantContext.setTenant("tenant-owner");
        when(userRoleRepository.findActiveRolesWithDetails(userId)).thenReturn(List.of(tenantAdminRole));
        when(publicSchemaUserLookupService.findInstitutionUserInPublicSchema(owner.getUsername(), organizationId))
                .thenReturn(Optional.of(owner));

        List<UserRole> roles = service.activeRolesForUser(userId, organizationId);

        assertEquals(Set.of(RoleType.ORGANIZATION_ADMIN, RoleType.ORGANIZATION_OWNER), roles.stream()
                .map(UserRole::getRole)
                .map(Role::getRoleType)
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals("tenant-owner", TenantContext.getTenant());
    }

    @Test
    void grantsElevatedTenantRolesOnlyTheAlreadyEntitledMenus() {
        Menu academics = Menu.builder()
                .id(18L)
                .menuName("Academics")
                .menuCode("ACADEMICS")
                .menuType(MenuType.MODULE)
                .build();
        Map<Long, Menu> entitled = Map.of(academics.getId(), academics);
        Map<Long, EffectivePermissionResponse> merged = new LinkedHashMap<>();

        service.applyElevatedTenantAdministratorBaseline(merged, entitled, true);

        assertEquals(1, merged.size());
        EffectivePermissionResponse permission = merged.get(18L);
        assertTrue(permission.getCanView());
        assertTrue(permission.getCanManage());
        assertTrue(permission.getCanApprove());

        Map<Long, EffectivePermissionResponse> noAccess = new LinkedHashMap<>();
        service.applyElevatedTenantAdministratorBaseline(noAccess, entitled, false);
        assertTrue(noAccess.isEmpty());
    }

    private UserRole role(Long userId, RoleType roleType, Long roleId) {
        Role role = new Role();
        role.setId(roleId);
        role.setRoleType(roleType);
        UserRole userRole = new UserRole();
        userRole.setRole(role);
        userRole.setActive(true);
        userRole.setPrimaryRole(true);
        userRole.setUser(new User());
        userRole.getUser().setId(userId);
        return userRole;
    }
}