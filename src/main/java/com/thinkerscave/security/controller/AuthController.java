package com.thinkerscave.security.controller;

import com.thinkerscave.platform.dto.response.PublicOrganizationOptionResponse;
import com.thinkerscave.platform.service.OrganizationService;
import com.thinkerscave.security.dto.LoginContext;
import com.thinkerscave.security.dto.request.LoginRequest;
import com.thinkerscave.security.dto.request.OtpResetPasswordRequest;
import com.thinkerscave.security.dto.response.AuthResponse;
import com.thinkerscave.security.service.AuthService;
import com.thinkerscave.security.service.PasswordResetService;
import com.thinkerscave.security.util.RefreshTokenCookieHelper;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.exceptions.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, token refresh and password reset")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenCookieHelper refreshTokenCookieHelper;
    private final OrganizationService organizationService;

    @GetMapping("/organizations")
    @Operation(summary = "List all active organizations for workspace selection",
            description = "Public endpoint (no auth required) for the org-select login screen")
    public ResponseEntity<ApiResponse<List<PublicOrganizationOptionResponse>>> getPublicOrganizations(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(
                "Organizations loaded",
                organizationService.listPublicOrganizations(search)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username/email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginContext loginContext = LoginContext.fromHeaders(
                httpRequest.getHeader(LoginContext.HEADER),
                httpRequest.getHeader("X-Tenant-ID"),
                httpRequest.getHeader("X-Organization-ID"));
        AuthResponse authResponse = authService.login(request, loginContext);
        return ResponseEntity.ok(ApiResponse.success("Login successful", applyRefreshCookie(authResponse, httpResponse)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token cookie or legacy query param")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestParam(required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String token = refreshTokenCookieHelper.resolveRefreshToken(httpRequest, refreshToken);
        if (!StringUtils.hasText(token)) {
            throw new BadRequestException("Refresh token is required");
        }
        AuthResponse authResponse = authService.refreshToken(
                token,
                httpRequest.getHeader("X-Tenant-ID"),
                parsePositiveLong(httpRequest.getHeader("X-Organization-ID")));
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", applyRefreshCookie(authResponse, httpResponse)));
    }

    private Long parsePositiveLong(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestParam(required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String token = refreshTokenCookieHelper.resolveRefreshToken(httpRequest, refreshToken);
        if (StringUtils.hasText(token)) {
            authService.logout(token);
        }
        refreshTokenCookieHelper.clearRefreshTokenCookie(httpResponse);
        return ResponseEntity.ok(ApiResponse.noContent("Logged out successfully"));
    }

    /**
     * When cookie mode is enabled, set HttpOnly cookie and omit refreshToken from JSON
     * so browser JavaScript never receives it.
     */
    private AuthResponse applyRefreshCookie(AuthResponse authResponse, HttpServletResponse httpResponse) {
        if (refreshTokenCookieHelper.isEnabled() && authResponse != null
                && StringUtils.hasText(authResponse.getRefreshToken())) {
            refreshTokenCookieHelper.setRefreshTokenCookie(
                    httpResponse, authResponse.getRefreshToken(), authResponse.getRememberMe());
            authResponse.setRefreshToken(null);
        }
        return authResponse;
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
