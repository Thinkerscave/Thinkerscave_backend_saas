package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.repository.RoleRepository;
import com.thinkerscave.common.security.UserInfoUserDetails;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.dto.UserResponseDTO;
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

    /**
     * Registers a new user with encrypted password.
     *
     * @param user the user entity
     * @return the saved user entity
     */
    @Transactional
    public User registerUser(User user) {
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
                roleNames);

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
                                .map(Role::getRoleName) // assuming Role has getName()
                                .toList())
                        .build());
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

            return userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to save user ({}): {}", context.email(), e.getMessage(), e);
            throw new RuntimeException("Failed to create user. Please check the input.");
        }
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

}
