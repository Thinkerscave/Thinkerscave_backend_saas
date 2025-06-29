package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import com.thinkerscave.common.usrm.service.LoginAttemptService;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final int MAX_ATTEMPTS = 3;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public void loginSucceeded(String userName) {
        userRepository.findByUserName(userName).ifPresent(user -> {
            user.setAttempts(null);
            user.setIsBlocked(false);
            user.setLockDateTime(null);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void loginFailed(String userName) {
        userRepository.findByUserName(userName).ifPresent(user -> {
            int attempts = user.getAttempts() + 1;
            user.setAttempts(attempts);

            if (attempts >= MAX_ATTEMPTS) {
                user.setIsBlocked(true);
                user.setLockDateTime(LocalDateTime.now());
            }

            userRepository.save(user);
        });
    }

    @Override
    public boolean isBlocked(String userName) {
        return userRepository.findByUserName(userName)
                .map(User::getIsBlocked)
                .orElse(false);
    }
}
