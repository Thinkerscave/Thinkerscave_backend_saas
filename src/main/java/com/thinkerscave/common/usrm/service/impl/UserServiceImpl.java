package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.role.domain.Role;
import com.thinkerscave.common.role.repository.RoleRepository;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.dto.UserResponseDTO;
import com.thinkerscave.common.usrm.repository.UserRepository;
import com.thinkerscave.common.usrm.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;
    /**
     * Registers a new user with encrypted password.
     *
     * @param user the user entity
     * @return the saved user entity
     */
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
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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
        	    roleNames
        	);

    }
}
