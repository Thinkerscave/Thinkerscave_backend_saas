package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.dto.UserProvisioningResult;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.entity.UserRole;
import com.thinkerscave.access.enums.UserStatus;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.UserService;
import com.thinkerscave.shared.enums.CodeType;
import com.thinkerscave.shared.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User createUser(UserCreationContext context, Role role) {
        return createUserWithTemporaryPassword(context, role).getUser();
    }

    @Override
    @Transactional
    public UserProvisioningResult createUserWithTemporaryPassword(UserCreationContext context, Role role) {
        log.info("Creating user for email: {}", context.email());

        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String userCode = codeGeneratorService.generate(CodeType.USER);

        // Generate a unique email if not provided
        String email = context.email();
        if (email == null || email.isBlank()) {
            String base = context.firstName() != null ? context.firstName().toLowerCase().replaceAll("[^a-z0-9]", "") : "user";
            if (context.lastName() != null && !context.lastName().isBlank()) {
                base += "." + context.lastName().toLowerCase().replaceAll("[^a-z0-9]", "");
            }
            if (context.mobileNumber() != null && !context.mobileNumber().isBlank()) {
                base += "." + context.mobileNumber().replaceAll("[^0-9]", "");
            } else {
                base += "." + userCode.toLowerCase().replaceAll("[^a-z0-9]", "");
            }
            email = base + "@thinkerscave.noreply";
        }

        String displayName = context.firstName() != null
                ? context.firstName() + (context.lastName() != null ? " " + context.lastName() : "")
                : email;

        User user = User.builder()
                .userCode(userCode)
                .email(email)
                .username(email)
                .mobileNumber(context.mobileNumber())
                .password(passwordEncoder.encode(tempPassword))
                .firstName(context.firstName() != null ? context.firstName() : "")
                .lastName(context.lastName() != null ? context.lastName() : context.defaultLastName())
                .displayName(displayName)
                .organizationId(0L) // Resolved at provisioning time
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
        user.getUserRoles().add(userRole);

        log.info("User created: code={}, email={}", userCode, context.email());
        return new UserProvisioningResult(user, tempPassword);
    }
}
