package com.thinkerscave.security.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.security.entity.PasswordResetToken;
import com.thinkerscave.security.repository.PasswordResetTokenRepository;
import com.thinkerscave.security.service.OutboundMessageService;
import com.thinkerscave.security.service.PasswordResetService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int OTP_EXPIRY_MINUTES = 10;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final OutboundMessageService outboundMessageService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.password-reset.log-otp:false}")
    private boolean logOtp;

    @Override
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email == null ? "" : email.trim());
        // Security: always return success — do not reveal if email exists
        if (userOpt.isEmpty()) {
            log.debug("Forgot password requested for unknown account");
            return;
        }
        User user = userOpt.get();
        String otp = generateOtp();

        tokenRepository.deleteByUser(user);

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(otp)
                .expirationDate(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        tokenRepository.save(token);

        outboundMessageService.sendPasswordResetOtp(
                user.getEmail(),
                user.getMobileNumber(),
                user.getFirstName(),
                otp);

        if (logOtp) {
            log.warn("Password-reset OTP for {} / mobile={} = {} (valid {} minutes). Enable only in non-prod.",
                    user.getEmail(), user.getMobileNumber(), otp, OTP_EXPIRY_MINUTES);
        }
        log.info("Password reset OTP dispatched for user id={}", user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyOtp(String email, String otp) {
        User user = findUserByEmail(email);
        validateToken(user, otp);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }
        User user = findUserByEmail(email);
        validateToken(user, otp);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFirstTimeLogin(false);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        tokenRepository.deleteByUser(user);
        log.info("Password reset successfully for user id={}", user.getId());
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));
    }

    private void validateToken(User user, String otp) {
        PasswordResetToken token = tokenRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));
        if (!token.getToken().equals(otp) || token.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired OTP");
        }
    }

    private String generateOtp() {
        return new SecureRandom().ints(0, 10)
                .limit(6)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
    }
}
