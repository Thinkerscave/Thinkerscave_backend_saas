package com.thinkerscave.security.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.security.entity.PasswordResetToken;
import com.thinkerscave.security.repository.PasswordResetTokenRepository;
import com.thinkerscave.security.service.EmailService;
import com.thinkerscave.security.service.PasswordResetService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase());
        // Security: always return success — do not reveal if email exists
        if (userOpt.isEmpty()) {
            log.debug("Forgot password requested for unknown account");
            return;
        }
        User user = userOpt.get();
        String otp = generateOtp();

        // Remove any existing token for this user
        tokenRepository.deleteByUser(user);

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(otp)
                .expirationDate(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        tokenRepository.save(token);

        String subject = "Your ThinkersCave Password Reset OTP";
        String body = emailService.buildOtpEmailBody(user.getFirstName(), otp);
        emailService.sendHtmlEmail(user.getEmail(), subject, body);
        log.info("Password reset OTP sent for user id={}", user.getId());
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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
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
