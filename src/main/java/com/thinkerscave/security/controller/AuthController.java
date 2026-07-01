package com.thinkerscave.security.controller;

import com.thinkerscave.security.dto.request.LoginRequest;
import com.thinkerscave.security.dto.request.OtpResetPasswordRequest;
import com.thinkerscave.security.dto.response.AuthResponse;
import com.thinkerscave.security.service.AuthService;
import com.thinkerscave.security.service.PasswordResetService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, token refresh and password reset")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    @Operation(summary = "Login with username/email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authService.refreshToken(refreshToken)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current session")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestParam String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.noContent("Logged out successfully"));
    }

    // ----------------------------------------------------------------
    // Self-service password reset (OTP-based)
    // ----------------------------------------------------------------

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset OTP",
            description = "Sends a 6-digit OTP to the registered email. Always returns 200 for security.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestParam @Email @NotBlank String email) {
        passwordResetService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.noContent(
                "If that email is registered, an OTP has been sent."));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify the OTP received by email")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @RequestParam @Email @NotBlank String email,
            @RequestParam @NotBlank @Size(min = 6, max = 6) String otp) {
        passwordResetService.verifyOtp(email, otp);
        return ResponseEntity.ok(ApiResponse.noContent("OTP verified successfully"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using verified OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody OtpResetPasswordRequest request) {
        passwordResetService.resetPassword(
                request.getEmail(), request.getOtp(),
                request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok(ApiResponse.noContent("Password reset successfully"));
    }
}
