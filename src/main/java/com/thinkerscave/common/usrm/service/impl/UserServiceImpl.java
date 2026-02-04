package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.repository.RoleRepository;
import com.thinkerscave.common.multitenancy.TenantContext;
import com.thinkerscave.common.orgm.service.SchemaInitializer;
import com.thinkerscave.common.security.UserInfoUserDetails;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.dto.UserResponseDTO;
import com.thinkerscave.common.usrm.repository.PasswordResetTokenRepository;
import com.thinkerscave.common.usrm.repository.UserRepository;
import com.thinkerscave.common.usrm.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private SchemaInitializer schemaInitializer;

    private User cloneUser(User source) {

        User target = new User();
        target.setUserCode(source.getUserCode());
        target.setFirstName(source.getFirstName());
        target.setMiddleName(source.getMiddleName());
        target.setLastName(source.getLastName());
        target.setEmail(source.getEmail());
        target.setMobileNumber(source.getMobileNumber());
        target.setUserName(source.getUserName());
        target.setPassword(source.getPassword());
        target.setAddress(source.getAddress());
        target.setCity(source.getCity());
        target.setState(source.getState());
        target.setIsBlocked(source.getIsBlocked());
        target.setIs2faEnabled(source.getIs2faEnabled());
        target.setMaxDeviceAllow(source.getMaxDeviceAllow());
        target.setAttempts(source.getAttempts());
        target.setLockDateTime(source.getLockDateTime());
        target.setSecretOperation(source.getSecretOperation());
        target.setRemarks(source.getRemarks());
        target.setSchemaName(source.getSchemaName());
        target.setIsFirstTimeLogin(source.getIsFirstTimeLogin());
        // =====================
        // Roles (IMPORTANT)
        // =====================
        // Reuse attached Role entities (roles SHOULD come from public schema)


        return target;
    }

    public List<Role> attachRolesPublicOnly(List<Role> roles) {

        try {
            // 🔐 Force PUBLIC schema for roles
            TenantContext.setCurrentTenant("public");

            return roles.stream()
                    .map(role ->
                            roleRepository.findByRoleCode(role.getRoleCode())
                                    .orElseThrow(() ->
                                            new IllegalStateException(
                                                    "Role not found in public schema: " + role.getRoleCode()
                                            )
                                    )
                    )
                    .collect(Collectors.toList());

        } finally {
            TenantContext.clear();
        }
    }


    /**
     * Registers a new user with encrypted password.
     *
     * @param user the user entity
     * @return the saved user entity
     */
    public User registerUser(User user) {
        String schema = user.getSchemaName();
        if (schema == null || schema.isBlank()) {
            schema = "public";
        }
        user.setSchemaName(schema);

        if (!"public".equalsIgnoreCase(schema)) {
            try {
                schemaInitializer.createAndInitializeSchema(schema);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize schema: " + schema, e);
            }
        }

        TenantContext.setCurrentTenant(schema);
        try {
            // Set encoded password
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            List<Role> attachedRoles = user.getRoles().stream().map(role -> {
                // Try to fetch role from DB by roleCode
                return roleRepository.findByRoleCode(role.getRoleCode()).orElseGet(() -> {
                    // If role doesn't exist, save it
                    return roleRepository.save(role);
                });
            }).collect(Collectors.toList());
            user.setRoles(attachedRoles);

            return userRepository.save(user);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void saveUserInSchemaAsync(User user, String schema) {

        logger.info("⏳ [SCHEMA-SAVE] Async user save initiated | schema={}", schema);

        CompletableFuture.runAsync(() -> {
            try {
                // 1️⃣ Set tenant context
                TenantContext.setCurrentTenant(schema);
                logger.info("🔁 [TENANT-CONTEXT] Tenant context set | schema={}", schema);


                // 2️⃣ Clone user (IMPORTANT)
                User clonedUser = cloneUser(user);

                List<Role> managedRoles = user.getRoles().stream()
                        .map(r ->
                                roleRepository.findByRoleCode(r.getRoleCode())
                                        .orElseThrow(() ->
                                                new IllegalStateException(
                                                        "Role not found in schema " + schema +
                                                                " role=" + r.getRoleCode()
                                                )
                                        )
                        )
                        .toList();

                clonedUser.setRoles(managedRoles);

                // 3️⃣ Save user in the given schema
                userRepository.save(clonedUser);

                logger.info("✅ [SCHEMA-SAVE] User saved successfully | schema={} | username={}",
                        schema, clonedUser.getUserName());

            } catch (Exception ex) {
                // Async failure should not crash main flow
                logger.error("❌ [SCHEMA-SAVE] Failed to save user | schema={}", schema, ex);

            } finally {
                // 4️⃣ Clear tenant context (MANDATORY)
                TenantContext.clear();
                logger.info("🧹 [TENANT-CONTEXT] Tenant context cleared | schema={}", schema);
            }

        }, executorService).exceptionally(ex -> {
            logger.error("❌ [ASYNC-EXECUTOR] Unexpected async failure | schema={}", schema, ex);
            return null;
        });
    }




    /**
     * Returns all users in the system as full entities (if needed internally).
     *
     * @return list of User entities
     */
    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves all users and maps them to response DTOs.
     *
     * @return list of UserResponseDTO
     */
    public List<UserResponseDTO> listUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a user by ID and maps it to a response DTO.
     *
     * @param id the user ID
     * @return optional UserResponseDTO
     */
    public Optional<UserResponseDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::mapToUserResponseDTO);
    }

    /**
     * Finds a user by email.
     *
     * @param email user email
     * @return optional User entity
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Updates user password and persists the change.
     *
     * @param user        the user
     * @param newPassword the new raw password
     */
    @Transactional
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    @Transactional // This is the crucial annotation
    public void updatePasswordAndInvalidateToken(User user, String newPassword) {
        // 1. Update the user's password with the encoded version
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 2. Delete the user's password reset token
        // This now works because it's inside a transaction
        passwordResetTokenRepository.deleteByUser(user);
    }

    /**
     * Converts a User entity to a UserResponseDTO.
     *
     * @param user the User entity
     * @return UserResponseDTO
     */
    private UserResponseDTO mapToUserResponseDTO(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        return new UserResponseDTO(
                user.getId(),
                user.getUserCode(),
                user.getUserName(),
                user.getEmail(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getAddress(),
                user.getCity(),
                user.getState(),
                user.getMobileNumber(),
                user.getIsBlocked(),
                user.getMaxDeviceAllow(),
                user.getIsFirstTimeLogin(),
                roleNames
        );

    }

    @Override
    public Optional<UserResponseDTO> findByUsername(String username) {
        return userRepository.findByUserName(username)
                .map(user -> UserResponseDTO.builder()
                        .id(user.getId())
                        .userCode(user.getUserCode())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .middleName(user.getMiddleName())
                        .lastName(user.getLastName())
                        .address(user.getAddress())
                        .city(user.getCity())
                        .state(user.getState())
                        .mobileNumber(user.getMobileNumber())
                        .isBlocked(user.getIsBlocked())
                        .maxDeviceAllow(user.getMaxDeviceAllow())
                        .firstTimeLogin(user.getIsFirstTimeLogin())
                        .roles(user.getRoles()
                                .stream()
                                .map(Role::getRoleName)   // assuming Role has getName()
                                .toList())
                        .build()
                );
    }

    public Long getCurrentUserRoleId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        UserInfoUserDetails userDetails = (UserInfoUserDetails) authentication.getPrincipal();
        return userDetails.getRoleId(); // now works ✅
    }

}
