package com.thinkerscave.security.service;

/**
 * OTP-based self-service password reset flow.
 */
public interface PasswordResetService {

    /**
     * Generates a 6-digit OTP for the given email and sends it asynchronously.
     * Always returns successfully — callers cannot distinguish missing accounts.
     */
    void forgotPassword(String email);

    /**
     * Verifies the OTP for the given email.
     *
     * @throws com.thinkerscave.shared.exceptions.BadRequestException if OTP is invalid or expired.
     */
    void verifyOtp(String email, String otp);

    /**
     * Resets the password after OTP verification.
     *
     * @throws com.thinkerscave.shared.exceptions.BadRequestException if OTP is invalid, expired, or passwords do not match.
     */
    void resetPassword(String email, String otp, String newPassword, String confirmPassword);
}
