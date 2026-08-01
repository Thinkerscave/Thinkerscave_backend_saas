package com.thinkerscave.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends transactional notices over free channels (email + optional SMS).
 * Used by password-reset OTP, customer welcome, and organization provisioning.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundMessageService {

    private final EmailService emailService;
    private final SmsService smsService;

    public void sendPasswordResetOtp(String email, String mobile, String firstName, String otp) {
        String subject = "Your ThinkersCave Password Reset OTP";
        String html = emailService.buildOtpEmailBody(firstName, otp);
        boolean mailOk = emailService.sendHtmlEmailSync(email, subject, html);

        String sms = "ThinkersCave OTP: " + otp + ". Valid for 10 minutes. Do not share this code.";
        boolean smsOk = trySms(mobile, sms);

        if (!mailOk && !smsOk) {
            log.error("Password-reset OTP could not be delivered by email or SMS for {}", email);
        } else {
            log.info("Password-reset OTP dispatched emailOk={} smsOk={} email={}", mailOk, smsOk, email);
        }
    }

    public void sendCustomerWelcome(
            String email,
            String mobile,
            String customerName,
            String ownerName,
            String loginUrl,
            String username,
            String temporaryPassword) {
        String subject = "Welcome to ThinkersCave";
        String html = emailService.buildCustomerWelcomeEmailBody(
                customerName, ownerName, loginUrl, username, temporaryPassword);
        boolean mailOk = emailService.sendHtmlEmailSync(email, subject, html);

        String sms = "Welcome to ThinkersCave (" + safe(customerName) + "). "
                + "Username: " + safe(username)
                + " Temp password: " + safe(temporaryPassword)
                + " Login: " + safe(loginUrl);
        boolean smsOk = trySms(mobile, sms);
        log.info("Customer welcome dispatched emailOk={} smsOk={} email={}", mailOk, smsOk, email);
    }

    public void sendOrganizationOwnerReady(
            String email,
            String mobile,
            String ownerName,
            String organizationName,
            String workspaceUrl) {
        String subject = "Organization created successfully";
        String html = emailService.buildOrganizationProvisionedOwnerEmailBody(
                ownerName, organizationName, workspaceUrl);
        boolean mailOk = emailService.sendHtmlEmailSync(email, subject, html);

        String sms = "ThinkersCave: Organization '" + safe(organizationName)
                + "' is ready. Workspace: " + safe(workspaceUrl);
        boolean smsOk = trySms(mobile, sms);
        log.info("Org owner notice dispatched emailOk={} smsOk={} email={}", mailOk, smsOk, email);
    }

    public void sendOrganizationAdminWelcome(
            String email,
            String mobile,
            String adminName,
            String organizationName,
            String workspaceUrl,
            String username,
            String temporaryPassword) {
        String subject = "Welcome to " + organizationName;
        String html = emailService.buildOrganizationAdminWelcomeEmailBody(
                adminName, organizationName, workspaceUrl, username, temporaryPassword);
        boolean mailOk = emailService.sendHtmlEmailSync(email, subject, html);

        String sms = "ThinkersCave admin for " + safe(organizationName)
                + ". Username: " + safe(username)
                + " Temp password: " + safe(temporaryPassword)
                + " Login: " + safe(workspaceUrl);
        boolean smsOk = trySms(mobile, sms);
        log.info("Org admin welcome dispatched emailOk={} smsOk={} email={}", mailOk, smsOk, email);
    }

    private boolean trySms(String mobile, String message) {
        if (!smsService.isEnabled() || !StringUtils.hasText(mobile)) {
            return false;
        }
        try {
            return smsService.sendSms(mobile, message);
        } catch (Exception ex) {
            log.error("SMS dispatch failed: {}", ex.getMessage());
            return false;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
