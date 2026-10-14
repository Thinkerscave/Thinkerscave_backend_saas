package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.request.CreateRoleRequest;
import com.thinkerscave.access.dto.request.UpdateRoleRequest;
import com.thinkerscave.access.dto.request.UpdateRolePermissionsRequest;
import com.thinkerscave.access.dto.response.PermissionMatrixResponse;
import com.thinkerscave.access.dto.response.RoleResponse;
import com.thinkerscave.access.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Role management operations.
 */
public interface RoleService {

    RoleResponse createRole(CreateRoleRequest request);

    RoleResponse updateRole(Long roleId, UpdateRoleRequest request);

    RoleResponse getRoleById(Long roleId);

    List<RoleResponse> getAllActiveRoles();

    List<RoleResponse> getAllRoles();

    Page<RoleResponse> searchRoles(Boolean active, String search, Pageable pageable);

    void activateRole(Long roleId);

    void deactivateRole(Long roleId);

    List<UserSummaryResponse> getRoleUsers(Long roleId, Long organizationId);

    /**
     * Returns the role's single global (organization-independent) permission
     * matrix. The matrix is the authoritative source applied to every tenant.
     */
    PermissionMatrixResponse getPermissionMatrix(Long roleId);

    /**
     * Replaces the role's global permission matrix and propagates it to every
     * applicable active, provisioned tenant schema.
     */
    void updatePermissionMatrix(Long roleId, UpdateRolePermissionsRequest request);

    void removeRolePermission(Long roleId, Long menuId);
}
