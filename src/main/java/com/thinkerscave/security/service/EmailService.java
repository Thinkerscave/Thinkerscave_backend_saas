package com.thinkerscave.security.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Transactional HTML email sender (OTP, welcome, provisioning).
 * Configure free Gmail SMTP via MAIL_* env vars — see .env.example.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final Environment environment;

    /** Non-empty in dev/test: all mail goes here. Must stay empty in prod. */
    @Value("${app.mail.redirect-to:}")
    private String redirectTo;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:${spring.mail.username:noreply@thinkerscave.com}}")
    private String fromEmail;

    @Value("${app.notification.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Async fire-and-forget send (non-critical paths).
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        sendHtmlEmailSync(to, subject, htmlBody);
    }

    /**
     * Synchronous send with success flag — preferred for OTP / onboarding credentials.
     */
    public boolean sendHtmlEmailSync(String to, String subject, String htmlBody) {
        if (!emailEnabled) {
            log.warn("Email disabled (app.notification.email.enabled=false). to={} subject={}", to, subject);
            return false;
        }
        if (!StringUtils.hasText(to)) {
            log.warn("Email skipped: empty recipient. subject={}", subject);
            return false;
        }
        try {
            String from = resolveFrom();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(from);
            boolean redirected = StringUtils.hasText(redirectTo);
            String deliveredTo = redirected ? redirectTo.trim() : to.trim();
            String deliveredSubject = redirected ? "[To: " + to.trim() + "] " + subject : subject;
            helper.setTo(deliveredTo);
            helper.setSubject(deliveredSubject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("Email sent intendedTo={} deliveredTo={} from={} redirected={} subject={}",
                    to, deliveredTo, from, redirected, deliveredSubject);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to={} subject={} reason={}", to, subject, e.getMessage());
            return false;
        }
    }

    @PostConstruct
    void validateRedirectPolicy() {
        if (StringUtils.hasText(redirectTo) && environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("app.mail.redirect-to must be empty under the prod profile");
        }
    }

    private String resolveFrom() {
        if (StringUtils.hasText(fromEmail)) {
            return fromEmail.trim();
        }
        if (StringUtils.hasText(mailUsername)) {
            return mailUsername.trim();
        }
        return "noreply@thinkerscave.com";
    }

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

    public String buildCustomerWelcomeEmailBody(
            String customerName,
            String ownerName,
            String loginUrl,
            String username,
            String temporaryPassword) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f6fb; padding: 24px;">
                  <div style="max-width: 640px; margin: auto; background: #fff; border-radius: 10px; padding: 28px; border: 1px solid #e5e7eb;">
                    <h2 style="margin: 0 0 8px; color: #0f172a;">Welcome to ThinkersCave</h2>
                    <p style="margin: 0 0 18px; color: #334155;">Hello %s, your customer account for <strong>%s</strong> is ready.</p>
                    <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; margin-bottom: 16px;">
                      <p style="margin: 0 0 6px;"><strong>Login URL:</strong> <a href="%s">%s</a></p>
                      <p style="margin: 0 0 6px;"><strong>Username:</strong> %s</p>
                      <p style="margin: 0;"><strong>Temporary Password:</strong> %s</p>
                    </div>
                    <p style="margin: 0 0 8px; color: #334155;">Please sign in and change your password immediately from your profile/password screen.</p>
                    <p style="margin: 0; color: #64748b; font-size: 12px;">If you did not expect this email, contact support.</p>
                  </div>
                </body>
                </html>
                """.formatted(
                ownerName != null ? ownerName : "Customer Owner",
                customerName != null ? customerName : "your organization",
                loginUrl,
                loginUrl,
                username,
                temporaryPassword
        );
    }

    public String buildOrganizationProvisionedOwnerEmailBody(String ownerName, String organizationName, String workspaceUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f6fb; padding: 24px;">
                  <div style="max-width: 640px; margin: auto; background: #fff; border-radius: 10px; padding: 28px; border: 1px solid #e5e7eb;">
                    <h2 style="margin: 0 0 8px; color: #0f172a;">Organization Provisioned</h2>
                    <p style="margin: 0 0 14px; color: #334155;">Hello %s, your workspace is now ready.</p>
                    <p style="margin: 0 0 6px;"><strong>Organization:</strong> %s</p>
                    <p style="margin: 0 0 16px;"><strong>Workspace URL:</strong> <a href="%s">%s</a></p>
                    <p style="margin: 0; color: #334155;">You can switch between your organizations from the workspace switcher after login.</p>
                  </div>
                </body>
                </html>
                """.formatted(
                ownerName != null ? ownerName : "Customer Owner",
                organizationName,
                workspaceUrl,
                workspaceUrl
        );
    }

    public String buildAdminPasswordResetEmailBody(
            String userName,
            String loginUrl,
            String username,
            String temporaryPassword) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f6fb; padding: 24px;">
                  <div style="max-width: 640px; margin: auto; background: #fff; border-radius: 10px; padding: 28px; border: 1px solid #e5e7eb;">
                    <h2 style="margin: 0 0 8px; color: #0f172a;">Your ThinkersCave Password Has Been Reset</h2>
                    <p style="margin: 0 0 18px; color: #334155;">Hello %s, an administrator has reset your account password.</p>
                    <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; margin-bottom: 16px;">
                      <p style="margin: 0 0 6px;"><strong>Login URL:</strong> <a href="%s">%s</a></p>
                      <p style="margin: 0 0 6px;"><strong>Username:</strong> %s</p>
                      <p style="margin: 0;"><strong>Temporary Password:</strong> %s</p>
                    </div>
                    <p style="margin: 0 0 8px; color: #334155;">On next login, you will be prompted to change this password before accessing the dashboard.</p>
                    <p style="margin: 0; color: #64748b; font-size: 12px;">If you did not expect this email, contact your organization administrator immediately.</p>
                  </div>
                </body>
                </html>
                """.formatted(
                userName != null ? userName : "there",
                loginUrl,
                loginUrl,
                username,
                temporaryPassword
        );
    }

    public String buildOrganizationAdminWelcomeEmailBody(
            String adminName,
            String organizationName,
            String loginUrl,
            String username,
            String temporaryPassword) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f6fb; padding: 24px;">
                  <div style="max-width: 640px; margin: auto; background: #fff; border-radius: 10px; padding: 28px; border: 1px solid #e5e7eb;">
                    <h2 style="margin: 0 0 8px; color: #0f172a;">Welcome to %s</h2>
                    <p style="margin: 0 0 18px; color: #334155;">Hello %s, your Organization Admin account is ready.</p>
                    <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; margin-bottom: 16px;">
                      <p style="margin: 0 0 6px;"><strong>Login URL:</strong> <a href="%s">%s</a></p>
                      <p style="margin: 0 0 6px;"><strong>Username:</strong> %s</p>
                      <p style="margin: 0;"><strong>Temporary Password:</strong> %s</p>
                    </div>
                    <p style="margin: 0 0 8px; color: #334155;">On first login, you will be prompted to change the password before accessing the dashboard.</p>
                    <p style="margin: 0; color: #64748b; font-size: 12px;">Use "Forgot password" if you need to reset credentials.</p>
                  </div>
                </body>
                </html>
                """.formatted(
                organizationName,
                adminName != null ? adminName : "Administrator",
                loginUrl,
                loginUrl,
                username,
                temporaryPassword
        );
    }

    public String buildInstitutionUserCredentialsEmailBody(
            String recipientName,
            String roleLabel,
            String organizationName,
            String loginUrl,
            String username,
            String temporaryPassword) {
        String org = HtmlUtils.htmlEscape(organizationName != null ? organizationName : "your organization");
        String name = HtmlUtils.htmlEscape(recipientName != null ? recipientName : "there");
        String role = HtmlUtils.htmlEscape(roleLabel != null ? roleLabel : "user");
        String user = HtmlUtils.htmlEscape(username != null ? username : "");
        String pwd = HtmlUtils.htmlEscape(temporaryPassword != null ? temporaryPassword : "");
        String url = HtmlUtils.htmlEscape(loginUrl != null ? loginUrl : "");
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"></head>
                <body style="margin: 0; padding: 24px; font-family: Arial, sans-serif; background-color: #f4f6fb;">
                  <div style="max-width: 640px; margin: auto; background: #ffffff; border-radius: 14px; overflow: hidden; border: 1px solid #e5e7eb;">
                    <div style="background: linear-gradient(135deg, #4f46e5, #2563eb); padding: 28px 32px; color: #ffffff;">
                      <p style="margin: 0 0 6px; font-size: 12px; letter-spacing: 1px; text-transform: uppercase; opacity: 0.85;">ThinkersCave</p>
                      <h2 style="margin: 0; font-size: 22px;">Welcome to %1$s</h2>
                    </div>
                    <div style="padding: 28px 32px;">
                      <p style="margin: 0 0 18px; color: #334155;">Hello %2$s, your %3$s account is ready. Sign in with the details below.</p>
                      <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 16px; margin-bottom: 20px;">
                        <p style="margin: 0 0 10px;"><strong>Username:</strong> %4$s</p>
                        <p style="margin: 0;"><strong>Temporary password:</strong> <span style="font-family: Consolas, monospace; background: #eef2ff; color: #3730a3; padding: 2px 8px; border-radius: 6px;">%5$s</span></p>
                      </div>
                      <div style="text-align: center; margin: 24px 0;">
                        <a href="%6$s" style="display: inline-block; background: #2563eb; color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 8px; font-weight: bold;">Sign in to your account</a>
                      </div>
                      <p style="margin: 0 0 8px; color: #334155;">You will be asked to set a new password on your first sign-in.</p>
                      <p style="margin: 0; color: #64748b; font-size: 12px;">Login link: <a href="%6$s" style="color: #2563eb;">%6$s</a></p>
                    </div>
                    <div style="padding: 16px 32px; background: #f8fafc; color: #94a3b8; font-size: 12px;">If you did not expect this email, contact your organization administrator.</div>
                  </div>
                </body>
                </html>
                """.formatted(org, name, role, user, pwd, url);
    }
}
