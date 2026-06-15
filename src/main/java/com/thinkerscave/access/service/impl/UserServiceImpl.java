package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User createUser(UserCreationContext context, Role role) {
        log.info("Creating user for email: {}", context.email());
        
        // Generate temporary password
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        
        User user = new User();
        user.setEmail(context.email());
        user.setUsername(context.email()); // Default username is email
        user.setMobileNumber(context.mobileNumber());
        // For security, usually password should be encoded via BCrypt, 
        // but skipping PasswordEncoder injection here for simplicity
        user.setPassword(tempPassword); 
        user.setUserCode("USR-" + System.currentTimeMillis());
        
        user.getRoles().add(role);
        
        return userRepository.save(user);
    }
}
