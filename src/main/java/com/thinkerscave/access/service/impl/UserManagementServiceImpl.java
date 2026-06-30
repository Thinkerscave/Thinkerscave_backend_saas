package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.*;
import com.thinkerscave.access.dto.response.*;
import com.thinkerscave.access.entity.*;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.enums.UserStatus;
import com.thinkerscave.access.mapper.UserMapper;
import com.thinkerscave.access.repository.*;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.access.service.UserManagementService;
import com.thinkerscave.shared.enums.CodeType;
import com.thinkerscave.shared.exceptions.*;
import com.thinkerscave.shared.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserMapper userMapper;
    private final CodeGeneratorService codeGeneratorService;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    @Override
    @Transactional
    public UserSummaryResponse createUser(Long organizationId, CreateUserRequest request) {
        validateUniqueEmail(request.getEmail(), null);
        String username = resolveUsername(request);
        validateUniqueUsername(username, null);

        Role role = roleRepository.findByRoleType(request.getRoleType())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found for type: " + request.getRoleType()));

        String userCode = codeGeneratorService.generate(CodeType.USER);
        String displayName = buildDisplayName(request.getFirstName(), request.getLastName());
        String tempPassword = generateTempPassword();

        User user = User.builder()
                .organizationId(organizationId)
                .userCode(userCode)
                .username(username)
                .email(request.getEmail().toLowerCase())
                .mobileNumber(request.getMobileNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .displayName(displayName)
                .password(passwordEncoder.encode(tempPassword))
                .status(UserStatus.ACTIVE)
                .firstTimeLogin(true)
                .build();

        user = userRepository.save(user);

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .primaryRole(true)
                .active(true)
                .build();
        userRoleRepository.save(userRole);

        log.info("User created: code={}, org={}, role={}", userCode, organizationId, request.getRoleType());
        return userMapper.toSummary(userRepository.findById(user.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public UserSummaryResponse updateUser(Long organizationId, Long userId, UpdateUserRequest request) {
        User user = findUserInOrg(organizationId, userId);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (StringUtils.hasText(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName());
        } else {
            user.setDisplayName(buildDisplayName(request.getFirstName(), request.getLastName()));
        }
        if (StringUtils.hasText(request.getMobileNumber())) {
            user.setMobileNumber(request.getMobileNumber());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }
        return userMapper.toSummary(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserById(Long organizationId, Long userId) {
        return userMapper.toSummary(findUserInOrg(organizationId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserByCode(Long organizationId, String userCode) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userCode));
        assertBelongsToOrg(user, organizationId);
        return userMapper.toSummary(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> searchUsers(Long organizationId, UserStatus status, RoleType roleType, String search, Pageable pageable) {
        var spec = com.thinkerscave.access.specification.UserSpecification.filter(organizationId, status, roleType, search);
        return userRepository.findAll(spec, pageable).map(userMapper::toSummary);
    }

    @Override
    @Transactional
    public void activateUser(Long organizationId, Long userId) {
        User user = findUserInOrg(organizationId, userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setAccountLocked(false);
        userRepository.save(user);
        log.info("User activated: id={}, org={}", userId, organizationId);
    }

    @Override
    @Transactional
    public void deactivateUser(Long organizationId, Long userId) {
        User user = findUserInOrg(organizationId, userId);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("User deactivated: id={}, org={}", userId, organizationId);
    }

    @Override
    @Transactional
    public void lockUser(Long organizationId, Long userId) {
        User user = findUserInOrg(organizationId, userId);
        user.setAccountLocked(true);
        user.setLockedAt(LocalDateTime.now());
        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);
        log.warn("User locked manually: id={}, org={}", userId, organizationId);
    }

    @Override
    @Transactional
    public void unlockUser(Long organizationId, Long userId) {
        User user = findUserInOrg(organizationId, userId);
        user.setAccountLocked(false);
        user.setLockedAt(null);
        user.setLockExpiryAt(null);
        user.setFailedLoginAttempts(0);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User unlocked: id={}, org={}", userId, organizationId);
    }

    @Override
    @Transactional
    public void resetPassword(Long organizationId, Long userId) {
        User user = findUserInOrg(organizationId, userId);
        String tempPassword = generateTempPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setFirstTimeLogin(true);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Password reset for user: id={}, org={}", userId, organizationId);
        // TODO: send email with tempPassword
    }

    @Override
    @Transactional
    public void changePassword(Long organizationId, Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }
        User user = findUserInOrg(organizationId, userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstTimeLogin(false);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Password changed by user: id={}", userId);
    }

    @Override
    @Transactional
    public void assignRole(Long organizationId, Long userId, Long roleId) {
        User user = findUserInOrg(organizationId, userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        if (userRoleRepository.existsByUser_IdAndRole_IdAndActiveTrue(userId, roleId)) {
            throw new AlreadyExistsException("Role already assigned to this user");
        }

        boolean hasPrimary = userRoleRepository.findByUser_IdAndPrimaryRoleTrue(userId).isPresent();
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .primaryRole(!hasPrimary)
                .active(true)
                .build();
        userRoleRepository.save(userRole);
        log.info("Role {} assigned to user {}", roleId, userId);
    }

    @Override
    @Transactional
    public void removeRole(Long organizationId, Long userId, Long roleId) {
        findUserInOrg(organizationId, userId);
        UserRole userRole = userRoleRepository.findByUser_IdAndRole_IdAndActiveTrue(userId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role assignment not found"));

        if (userRole.getPrimaryRole()) {
            long activeCount = userRoleRepository.findActiveRolesWithDetails(userId).size();
            if (activeCount <= 1) {
                throw new ConflictException("Cannot remove the last role from a user");
            }
        }
        userRole.setActive(false);
        userRoleRepository.save(userRole);
        log.info("Role {} removed from user {}", roleId, userId);
    }

    @Override
    @Transactional
    public void setPrimaryRole(Long organizationId, Long userId, Long roleId) {
        findUserInOrg(organizationId, userId);
        // Clear existing primary
        List<UserRole> activeRoles = userRoleRepository.findActiveRolesWithDetails(userId);
        activeRoles.forEach(ur -> ur.setPrimaryRole(ur.getRole().getId().equals(roleId)));
        userRoleRepository.saveAll(activeRoles);
    }

    @Override
    @Transactional
    public void bulkUpdateStatus(Long organizationId, BulkUserStatusRequest request) {
        List<User> users = userRepository.findAllById(request.getUserIds()).stream()
                .filter(u -> u.getOrganizationId().equals(organizationId))
                .toList();

        String action = request.getAction().toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        users.forEach(u -> {
            switch (action) {
                case "ACTIVATE"   -> { u.setStatus(UserStatus.ACTIVE); u.setAccountLocked(false); }
                case "DEACTIVATE" -> u.setStatus(UserStatus.INACTIVE);
                case "LOCK"       -> { u.setStatus(UserStatus.LOCKED); u.setAccountLocked(true); u.setLockedAt(now); }
                case "UNLOCK"     -> { u.setStatus(UserStatus.ACTIVE); u.setAccountLocked(false); u.setFailedLoginAttempts(0); }
                default           -> throw new BadRequestException("Unknown action: " + action);
            }
        });
        userRepository.saveAll(users);
        log.info("Bulk {} applied to {} users in org {}", action, users.size(), organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EffectivePermissionResponse> getEffectivePermissions(Long organizationId, Long userId) {
        findUserInOrg(organizationId, userId);
        return permissionService.getEffectivePermissions(userId, organizationId);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private User findUserInOrg(Long organizationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        assertBelongsToOrg(user, organizationId);
        return user;
    }

    private void assertBelongsToOrg(User user, Long organizationId) {
        if (!user.getOrganizationId().equals(organizationId)) {
            throw new ResourceNotFoundException("User not found in this organization");
        }
    }

    private void validateUniqueEmail(String email, Long excludeId) {
        if (excludeId == null ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, excludeId)) {
            throw new AlreadyExistsException("Email already in use: " + email);
        }
    }

    private void validateUniqueUsername(String username, Long excludeId) {
        if (excludeId == null ? userRepository.existsByUsername(username)
                : userRepository.existsByUsernameAndIdNot(username, excludeId)) {
            throw new AlreadyExistsException("Username already in use: " + username);
        }
    }

    private String resolveUsername(CreateUserRequest request) {
        return StringUtils.hasText(request.getUsername())
                ? request.getUsername()
                : request.getEmail().toLowerCase().split("@")[0];
    }

    private String buildDisplayName(String firstName, String lastName) {
        return StringUtils.hasText(lastName)
                ? firstName + " " + lastName
                : firstName;
    }

    private String generateTempPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "A1!";
    }
}
