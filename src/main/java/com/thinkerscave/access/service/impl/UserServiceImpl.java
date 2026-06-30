package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.entity.UserRole;
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
        log.info("Creating user for email: {}", context.email());

        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String userCode = codeGeneratorService.generate(CodeType.USER);

        String displayName = context.firstName() != null
                ? context.firstName() + (context.lastName() != null ? " " + context.lastName() : "")
                : context.email();

        User user = User.builder()
                .userCode(userCode)
                .email(context.email())
                .username(context.email())
                .mobileNumber(context.mobileNumber())
                .password(passwordEncoder.encode(tempPassword))
                .firstName(context.firstName() != null ? context.firstName() : "")
                .lastName(context.lastName() != null ? context.lastName() : context.defaultLastName())
                .displayName(displayName)
                .organizationId(0L) // Resolved at provisioning time
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
        return user;
    }
}
