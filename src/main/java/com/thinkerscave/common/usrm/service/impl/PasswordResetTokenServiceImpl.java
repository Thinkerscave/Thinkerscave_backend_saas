package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.usrm.domain.PasswordResetToken;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.PasswordResetTokenRepository;
import com.thinkerscave.common.usrm.service.PasswordResetTokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService{
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetToken createToken(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setExpirationDate(LocalDateTime.now().plusHours(24));
        token.setUser(user);
        return passwordResetTokenRepository.save(token);
    }

    public Optional<PasswordResetToken> validateToken(String token) {
        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);
        if (resetToken.isPresent() && resetToken.get().getExpirationDate().isAfter(LocalDateTime.now())) {
            return resetToken;
        }
        return Optional.empty();
    }
}
