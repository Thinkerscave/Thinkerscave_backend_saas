package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.menum.repository.RoleMenuPrivilegeMappingRepository;
import com.thinkerscave.common.menum.domain.RoleMenuPrivilegeMapping;
import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.repository.RoleRepository;
import com.thinkerscave.common.security.UserInfoUserDetails;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.dto.UserResponseDTO;
import com.thinkerscave.common.usrm.dto.UserRequestDTO;
import com.thinkerscave.common.usrm.dto.UserOrgDTO;
import com.thinkerscave.common.usrm.repository.PasswordResetTokenRepository;
import com.thinkerscave.common.usrm.repository.UserRepository;
import com.thinkerscave.common.usrm.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OrganizationRepository organizationRepository;
    private final EmailService emailService;
    private final RoleMenuPrivilegeMappingRepository roleMenuPrivilegeMappingRepository;

    /**
     * Registers a new user with encrypted password using a DTO.
     *
     * @param dto the user request DTO
     * @return the saved user as a Response DTO
     */
    @Override
    @Transactional
    public UserResponseDTO registerUser(UserRequestDTO dto) {
        User user = mapToEntity(dto);

        // Set encoded password
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // Auto-generate userCode if not supplied
        if (user.getUserCode() == null || user.getUserCode().isBlank()) {
            String safeUser = user.getUserName() != null ? user.getUserName().toLowerCase() : "user";
            user.setUserCode(safeUser + "_" + generateRandomAlphaNumeric(6));
        }

        // New users always start with first-time login flag
        user.setIsFirstTimeLogin(true);

        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            List<Role> attachedRoles = dto.getRoles().stream().map(roleName -> {
                return roleRepository.findByRoleName(roleName).orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName(roleName);
                    newRole.setRoleCode(roleName.toUpperCase().replace(" ", "_"));
                    return roleRepository.save(newRole);
                });
            }).collect(Collectors.toList());
            user.setRoles(attachedRoles);
        }

        if (dto.getOrganizationIds() != null && !dto.getOrganizationIds().isEmpty()) {
            List<Organisation> orgs = organizationRepository.findAllById(dto.getOrganizationIds());
            user.setOrganizations(orgs);
        }

        User savedUser = userRepository.save(user);
        return mapToUserResponseDTO(savedUser);
    }

    private User mapToEntity(UserRequestDTO dto) {
        return User.builder()
                .userName(dto.getUserName())
                .email(dto.getEmail())
                .firstName(dto.getFirstName())
                .middleName(dto.getMiddleName())
                .lastName(dto.getLastName())
                .mobileNumber(dto.getMobileNumber())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .zipCode(dto.getZipCode())
                .gender(dto.getGender())
                .dateOfBirth(dto.getDateOfBirth())
                .build();
    }



    /**
     * Retrieves all users and maps them to response DTOs.
     *
     * @return list of UserResponseDTO
     */
    @Override
    @Transactional(readOnly = true)
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

    @Transactional
    public void updatePasswordAndInvalidateToken(User user, String newPassword) {
        // 1. Update the user's password with the encoded version
        user.setPassword(passwordEncoder.encode(newPassword));
        // 2. Clear first-time-login flag so the user is not redirected again
        user.setIsFirstTimeLogin(false);
        userRepository.save(user);
        // 3. Delete the user's password reset token (one-time use)
        passwordResetTokenRepository.deleteByUser(user);
    }

    /**
     * Changes the password for an already-authenticated user (first-time login
     * flow).
     * Marks isFirstTimeLogin = false so the user reaches the dashboard next time.
     *
     * @param username    the authenticated username
     * @param newPassword the new raw password
     */
    @Transactional
    public void changePasswordForCurrentUser(String username, String newPassword) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setIsFirstTimeLogin(false);
        userRepository.save(user);
    }

    /**
     * Converts a User entity to a UserResponseDTO.
     *
     * @param user the User entity
     * @return UserResponseDTO
     */
    private UserResponseDTO mapToUserResponseDTO(User user) {
        List<com.thinkerscave.common.usrm.dto.InternalRoleDTO> roleDTOs = user.getRoles().stream()
                .map(r -> com.thinkerscave.common.usrm.dto.InternalRoleDTO.builder()
                        .roleCode(r.getRoleCode())
                        .roleName(r.getRoleName())
                        .description(r.getDescription())
                        .build())
                .collect(Collectors.toList());

        List<String> privileges = new java.util.ArrayList<>();
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            Role role = user.getRoles().iterator().next();
            List<RoleMenuPrivilegeMapping> mappings = roleMenuPrivilegeMappingRepository.findByRoleId(role.getRoleId());
            for (RoleMenuPrivilegeMapping mapping : mappings) {
                if (mapping.getSubMenu() != null && mapping.getPrivilege() != null) {
                    privileges.add(
                            mapping.getSubMenu().getSubMenuCode() + "_" + mapping.getPrivilege().getPrivilegeName());
                }
            }
        }

        List<UserOrgDTO> orgDTOs = user.getOrganizations().stream()
                .map(org -> new UserOrgDTO(org.getOrgId(), org.getOrgName(), org.getOrgCode()))
                .collect(Collectors.toList());

        return UserResponseDTO.builder()
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
                .roles(roleDTOs)
                .privileges(privileges)
                .orgType(getOrgTypeForCurrentTenant())
                .organizations(orgDTOs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponseDTO> findByUsername(String username) {
        return userRepository.findByUserName(username)
                .map(user -> {
                    List<UserOrgDTO> orgDTOs = user.getOrganizations().stream()
                            .map(org -> new UserOrgDTO(org.getOrgId(), org.getOrgName(), org.getOrgCode()))
                            .collect(Collectors.toList());

                    List<String> privileges = new java.util.ArrayList<>();
                    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                        Role role = user.getRoles().iterator().next();
                        List<RoleMenuPrivilegeMapping> mappings = roleMenuPrivilegeMappingRepository
                                .findByRoleId(role.getRoleId());
                        for (RoleMenuPrivilegeMapping mapping : mappings) {
                            if (mapping.getSubMenu() != null && mapping.getPrivilege() != null) {
                                privileges.add(mapping.getSubMenu().getSubMenuCode() + "_"
                                        + mapping.getPrivilege().getPrivilegeName());
                            }
                        }
                    }

                    return UserResponseDTO.builder()
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
                                    .map(r -> com.thinkerscave.common.usrm.dto.InternalRoleDTO.builder()
                                            .roleCode(r.getRoleCode())
                                            .roleName(r.getRoleName())
                                            .description(r.getDescription())
                                            .build())
                                    .toList())
                            .privileges(privileges)
                            .orgType(getOrgTypeForCurrentTenant())
                            .organizations(orgDTOs)
                            .build();
                });
    }

    public Long getCurrentUserRoleId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        UserInfoUserDetails userDetails = (UserInfoUserDetails) authentication.getPrincipal();
        return userDetails.getRoleId(); // now works ✅
    }

    @Override
    @Transactional
    public User createUser(com.thinkerscave.common.usrm.dto.UserCreationContext context, Role role) {
        User user = new User();
        try {
            user.setFirstName(context.firstName());
            user.setMiddleName(context.middleName());
            user.setLastName(context.lastName());
            user.setEmail(context.email());
            user.setMobileNumber(Long.parseLong(context.mobileNumber()));
            user.setAddress(context.address());
            user.setState(context.state());
            user.setCity(context.city());

            String safeFirst = context.firstName() != null ? context.firstName().trim().toLowerCase() : "user";
            String safeLast = context.lastName() != null ? context.lastName().trim().toLowerCase()
                    : context.defaultLastName();
            String userName = safeFirst + "_" + safeLast + generateRandomAlphaNumeric(3);
            String userCode = userName + "_" + generateRandomAlphaNumeric(5);
            String rawPassword = generateRandomAlphaNumeric(6);
            String encodedPassword = passwordEncoder.encode(rawPassword);

            user.setUserName(userName);
            user.setUserCode(userCode);
            user.setPassword(encodedPassword);
            user.setRoles(List.of(role));

            User savedUser = userRepository.save(user);

            // Send welcome email with temporary credentials (async — won't block or
            // rollback)
            sendWelcomeEmail(savedUser, rawPassword);

            return savedUser;
        } catch (Exception e) {
            log.error("Failed to save user ({}): {}", context.email(), e.getMessage(), e);
            throw new RuntimeException("Failed to create user. Please check the input.");
        }
    }

    /**
     * Sends a professional HTML welcome email containing the auto-generated
     * username and temporary password. The email is sent asynchronously via
     * {@link EmailService}, so SMTP failures never rollback the transaction.
     */
    private void sendWelcomeEmail(User user, String rawPassword) {
        try {
            String subject = "Welcome to ThinkersCave – Your Account is Ready";
            String html = buildWelcomeEmail(user.getFirstName(), user.getUserName(), rawPassword);
            emailService.sendHtmlEmail(user.getEmail(), subject, html);
            log.info("Welcome email dispatched to {}", user.getEmail());
        } catch (Exception e) {
            // Log but never throw — do not fail user creation due to email issues
            log.warn("Could not send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String buildWelcomeEmail(String firstName, String userName, String tempPassword) {
        return "<div style='font-family: Helvetica, Arial, sans-serif; max-width: 600px; margin: auto; line-height: 1.6; color: #333;'>"
                + "<div style='background: #00466a; padding: 20px; text-align: center;'>"
                + "  <h1 style='color: #fff; margin: 0;'>Welcome to ThinkersCave</h1>"
                + "</div>"
                + "<div style='padding: 30px;'>"
                + "  <p>Hi <strong>" + firstName + "</strong>,</p>"
                + "  <p>Your account has been created. Here are your login credentials:</p>"
                + "  <table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>"
                + "    <tr><td style='padding: 8px; font-weight: bold; width: 40%;'>Username</td>"
                + "         <td style='padding: 8px; background: #f4f4f4; font-family: monospace;'>" + userName
                + "</td></tr>"
                + "    <tr><td style='padding: 8px; font-weight: bold;'>Temporary Password</td>"
                + "         <td style='padding: 8px; background: #f4f4f4; font-family: monospace;'>" + tempPassword
                + "</td></tr>"
                + "  </table>"
                + "  <p style='color: #e74c3c;'><strong>Important:</strong> You will be asked to change your password the first time you log in.</p>"
                + "  <p>Regards,<br/>The ThinkersCave Team</p>"
                + "</div>"
                + "<div style='background: #f9f9f9; padding: 12px; text-align: center; font-size: 0.8em; color: #aaa;'>"
                + "  ThinkersCave Inc. &bull; Bhubaneswar, India"
                + "</div>"
                + "</div>";
    }

    private String generateRandomAlphaNumeric(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder(length);
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }

    private String getOrgTypeForCurrentTenant() {
        String tenantId = TenantContext.getTenant();
        if (tenantId != null && !"public".equals(tenantId)) {
            return organizationRepository.findByTenantSchema(tenantId)
                    .map(org -> org.getType() != null ? org.getType().name() : null)
                    .orElse(null);
        }
        return null;
    }

}
