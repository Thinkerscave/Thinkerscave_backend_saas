package com.thinkerscave.common.usrm.service.impl;

import org.springframework.transaction.annotation.Transactional;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import com.thinkerscave.common.usrm.service.LoginAttemptService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final int MAX_ATTEMPTS = 3;
    /** Auto-unlock after this many minutes */
    private static final int LOCK_DURATION_MINUTES = 15;

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
            int previousAttempts = user.getAttempts() != null ? user.getAttempts() : 0;
            int attempts = previousAttempts + 1;
            user.setAttempts(attempts);

            if (attempts >= MAX_ATTEMPTS) {
                user.setIsBlocked(true);
                user.setLockDateTime(LocalDateTime.now());
            }

            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public boolean isBlocked(String userName) {
        return userRepository.findByUserName(userName)
                .map(user -> {
                    if (!Boolean.TRUE.equals(user.getIsBlocked())) {
                        return false;
                    }
                    // Auto-unlock if lock has expired
                    if (user.getLockDateTime() != null
                            && user.getLockDateTime().plusMinutes(LOCK_DURATION_MINUTES)
                                    .isBefore(LocalDateTime.now())) {
                        user.setIsBlocked(false);
                        user.setAttempts(0);
                        user.setLockDateTime(null);
                        userRepository.save(user);
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }
}
