package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.ChangePasswordRequest;
import com.thinkerscave.access.dto.request.UpdateUserRequest;
import com.thinkerscave.access.dto.response.UserSummaryResponse;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.ProfileService;
import com.thinkerscave.access.service.UserManagementService;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserManagementService userManagementService;

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser() {
        User user = resolveCurrentUser();
        return userManagementService.getUserById(user.getOrganizationId(), user.getId());
    }

    @Override
    @Transactional
    public UserSummaryResponse updateCurrentUser(UpdateUserRequest request) {
        User user = resolveCurrentUser();
        return userManagementService.updateUser(user.getOrganizationId(), user.getId(), request);
    }

    @Override
    @Transactional
    public void changeCurrentUserPassword(ChangePasswordRequest request) {
        User user = resolveCurrentUser();
        userManagementService.changePassword(user.getOrganizationId(), user.getId(), request);
    }

    private User resolveCurrentUser() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new ResourceNotFoundException("Signed-in user not found"));
    }
}
