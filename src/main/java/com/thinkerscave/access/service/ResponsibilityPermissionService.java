package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.request.UpdateRolePermissionsRequest;
import com.thinkerscave.access.dto.response.PermissionMatrixResponse;

public interface ResponsibilityPermissionService {

    PermissionMatrixResponse getPermissionMatrix(Long responsibilityId, Long organizationId);

    void updatePermissionMatrix(Long responsibilityId, Long organizationId, UpdateRolePermissionsRequest request);
}
