package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.request.UpdateUserPermissionsRequest;
import com.thinkerscave.access.dto.response.EffectivePermissionResponse;

import java.util.List;

/**
 * Permission resolution and user-level permission overrides.
 */
public interface PermissionService {

    List<EffectivePermissionResponse> getEffectivePermissions(Long userId, Long organizationId);

    void updateUserPermissions(Long userId, Long organizationId, UpdateUserPermissionsRequest request);

    EffectivePermissionResponse checkPermission(Long userId, Long organizationId, Long menuId);

    boolean hasPermission(Long userId, Long organizationId, String menuCode, String privilege);
}
