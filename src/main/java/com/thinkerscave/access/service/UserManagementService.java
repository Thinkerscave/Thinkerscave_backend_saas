package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.request.*;
import com.thinkerscave.access.dto.response.*;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * User management operations for the access module.
 */
public interface UserManagementService {

    UserSummaryResponse createUser(Long organizationId, CreateUserRequest request);

    UserSummaryResponse updateUser(Long organizationId, Long userId, UpdateUserRequest request);

    UserSummaryResponse getUserById(Long organizationId, Long userId);

    UserSummaryResponse getUserByCode(Long organizationId, String userCode);

    Page<UserSummaryResponse> searchUsers(Long organizationId, UserStatus status, RoleType roleType, String search, Pageable pageable);

    void activateUser(Long organizationId, Long userId);

    void deactivateUser(Long organizationId, Long userId);

    void lockUser(Long organizationId, Long userId);

    void unlockUser(Long organizationId, Long userId);

    void resetPassword(Long organizationId, Long userId);

    void changePassword(Long organizationId, Long userId, ChangePasswordRequest request);

    void assignRole(Long organizationId, Long userId, Long roleId);

    void removeRole(Long organizationId, Long userId, Long roleId);

    void setPrimaryRole(Long organizationId, Long userId, Long roleId);

    void bulkUpdateStatus(Long organizationId, BulkUserStatusRequest request);

    List<EffectivePermissionResponse> getEffectivePermissions(Long organizationId, Long userId);
}
