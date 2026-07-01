package com.thinkerscave.security.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Async email service for transactional emails (OTP, welcome, etc.).
 * Falls back gracefully when mail is not configured (dev/test profiles).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@thinkerscave.com}")
    private String fromEmail;

    /**
     * Sends an HTML email asynchronously.
     *
     * @param to       Recipient email address
     * @param subject  Email subject
     * @param htmlBody HTML content
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Builds the HTML body for OTP emails.
     */
    public String buildOtpEmailBody(String firstName, String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                  <div style="max-width:600px; margin:auto; background:#fff; border-radius:8px; padding:32px;">
                    <h2 style="color:#1a73e8;">ThinkersCave — Password Reset OTP</h2>
                    <p>Dear %s,</p>
                    <p>Your one-time password (OTP) for resetting your ThinkersCave account password is:</p>
                    <div style="text-align:center; margin:24px 0;">
                      <span style="font-size:36px; font-weight:bold; letter-spacing:8px; color:#1a73e8;">%s</span>
                    </div>
                    <p>This OTP is valid for <strong>10 minutes</strong>. Do not share it with anyone.</p>
                    <p style="color:#888; font-size:12px;">If you did not request a password reset, please ignore this email.</p>
                  </div>
                </body>
                </html>
                """.formatted(firstName != null ? firstName : "User", otp);
    }
}
