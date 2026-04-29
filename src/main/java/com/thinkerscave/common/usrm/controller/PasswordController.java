package com.thinkerscave.common.usrm.controller;

import com.thinkerscave.common.usrm.domain.PasswordResetToken;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.dto.ResetPasswordRequest;
import com.thinkerscave.common.usrm.service.PasswordResetTokenService;
import com.thinkerscave.common.usrm.service.UserService;
import com.thinkerscave.common.usrm.service.impl.EmailService;
import com.thinkerscave.common.commonModel.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/password")
@Tag(name = "Password", description = "Endpoints related to password management (reset, update)")
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final UserService userService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;

    @Value("${server.port}")
    private String serverPort;

    /**
     * Step 1: User provides their email to receive an OTP.
     */
    @Operation(summary = "Forgot Password", description = "Initiate password reset by sending an OTP to the user's email.")
    @PostMapping("/forgot")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam("email") String email) {
        log.info("Password reset requested for email: {}", email);
        userService.findByEmail(email).ifPresent(passwordResetTokenService::createAndSendOtp);
        // For security, always return a positive message
        return ResponseEntity.ok(ApiResponse.success("If an account with that email exists, an OTP has been sent.", null));
    }

    @Operation(summary = "Verify OTP", description = "Verify the OTP sent to the user's email.")
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@RequestParam("email") String email, @RequestParam("otp") String otp) {
        return userService.findByEmail(email)
            .flatMap(user -> passwordResetTokenService.validateOtp(user, otp))
            .map(token -> ResponseEntity.ok(ApiResponse.<Void>success("OTP verified successfully.", null)))
            .orElseGet(() -> ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired OTP.")));
    }

    /**
     * Step 3: User provides email, the verified OTP, and the new password.
     */
    @Operation(summary = "Reset Password", description = "Reset the password using the verified OTP.")
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return userService.findByEmail(request.getEmail())
            .flatMap(user -> passwordResetTokenService.validateOtp(user, request.getOtp())
                .map(token -> {
                    userService.updatePasswordAndInvalidateToken(user, request.getNewPassword());
                    return ResponseEntity.ok(ApiResponse.<Void>success("Password reset successful.", null));
                }))
            .orElseGet(() -> ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired OTP.")));
    }
}
