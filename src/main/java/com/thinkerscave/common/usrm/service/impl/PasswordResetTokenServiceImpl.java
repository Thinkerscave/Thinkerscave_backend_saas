package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.usrm.domain.PasswordResetToken;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.PasswordResetTokenRepository;
import com.thinkerscave.common.usrm.service.PasswordResetTokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService{
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private EmailService emailService;

//    public PasswordResetToken createToken(User user) {
//        PasswordResetToken token = new PasswordResetToken();
//        token.setToken(UUID.randomUUID().toString());
//        token.setExpirationDate(LocalDateTime.now().plusHours(24));
//        token.setUser(user);
//        return passwordResetTokenRepository.save(token);
//    }
//
//    public Optional<PasswordResetToken> validateToken(String token) {
//        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);
//        if (resetToken.isPresent() && resetToken.get().getExpirationDate().isAfter(LocalDateTime.now())) {
//            return resetToken;
//        }
//        return Optional.empty();
//    }
@Override
@Transactional
public PasswordResetToken createAndSendOtp(User user) {
    // 1. Generate a 6-digit numeric OTP
    String otp = new SecureRandom().ints(0, 10)
            .limit(6)
            .mapToObj(String::valueOf)
            .collect(Collectors.joining());

    // 2. Delete any existing token for this user to avoid conflicts
    passwordResetTokenRepository.deleteByUser(user);

    // 3. Create the new token object
    PasswordResetToken token = new PasswordResetToken();
    token.setToken(otp);
    token.setExpirationDate(LocalDateTime.now().plusMinutes(10)); // OTP is valid for 10 minutes
    token.setUser(user);

    // 4. Build the professional HTML email content
    String emailSubject = "Your One-Time Password (OTP) for ThinkersCave";
    String htmlContent = buildOtpEmail(user.getFirstName(), otp);

    // --- FIX IS HERE ---
    // 5. Send ONLY the professional HTML email. The duplicate call has been removed.
    emailService.sendHtmlEmail(user.getEmail(), emailSubject, htmlContent);

    // 6. Save the new token to the database and return it
    return passwordResetTokenRepository.save(token);
}

    @Override
    @Transactional
    public Optional<PasswordResetToken> validateOtp(User user, String otp) {
        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findByUser(user);

        // --- Start Debugging ---
        System.out.println("--- OTP Validation ---");
        System.out.println("Current Server Time: " + LocalDateTime.now());

        if (resetTokenOpt.isPresent()) {
            PasswordResetToken resetToken = resetTokenOpt.get();
            System.out.println("Token Found in DB: " + resetToken.getToken());
            System.out.println("Token Expiration Time: " + resetToken.getExpirationDate());
            System.out.println("Is token expired? " + resetToken.getExpirationDate().isBefore(LocalDateTime.now()));
            System.out.println("Does OTP match? " + resetToken.getToken().equals(otp));
        } else {
            System.out.println("No token found for user: " + user.getEmail());
        }
        System.out.println("--------------------");
        // --- End Debugging ---

        if (resetTokenOpt.isPresent() &&
                resetTokenOpt.get().getToken().equals(otp) &&
                resetTokenOpt.get().getExpirationDate().isAfter(LocalDateTime.now())) {

            return resetTokenOpt;
        }

        return Optional.empty();
    }
    /**
     * A private helper method to build a simple, clean HTML email template for the OTP.
     * @param userName The name of the user.
     * @param otp The 6-digit OTP.
     * @return A string containing the full HTML content for the email body.
     */
    private String buildOtpEmail(String userName, String otp) {
        return "<div style='font-family: Helvetica, Arial, sans-serif; min-width: 1000px; overflow: auto; line-height: 2;'>" +
                "  <div style='margin: 50px auto; width: 70%; padding: 20px 0;'>" +
                "    <div style='border-bottom: 1px solid #eee;'>" +
                "      <a href='' style='font-size: 1.4em; color: #00466a; text-decoration: none; font-weight: 600;'>ThinkersCave</a>" +
                "    </div>" +
                "    <p style='font-size: 1.1em;'>Hi, " + userName + ",</p>" +
                "    <p>Thank you for choosing ThinkersCave. Use the following OTP to complete your password reset procedures. OTP is valid for 10 minutes.</p>" +
                "    <h2 style='background: #00466a; margin: 0 auto; width: max-content; padding: 0 10px; color: #fff; border-radius: 4px;'>" + otp + "</h2>" +
                "    <p style='font-size: 0.9em;'>Regards,<br />The ThinkersCave Team</p>" +
                "    <hr style='border: none; border-top: 1px solid #eee;' />" +
                "    <div style='float: right; padding: 8px 0; color: #aaa; font-size: 0.8em; line-height: 1; font-weight: 300;'>" +
                "      <p>ThinkersCave Inc.</p>" +
                "      <p>Bhubaneswar, India</p>" +
                "    </div>" +
                "  </div>" +
                "</div>";
    }
}
