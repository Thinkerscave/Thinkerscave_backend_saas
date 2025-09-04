package com.thinkerscave.common.usrm.controller;

import com.thinkerscave.common.usrm.domain.PasswordResetToken;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.dto.ResetPasswordRequest;
import com.thinkerscave.common.usrm.repository.PasswordResetTokenRepository;
import com.thinkerscave.common.usrm.service.PasswordResetTokenService;
import com.thinkerscave.common.usrm.service.UserService;
import com.thinkerscave.common.usrm.service.impl.EmailService;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/password")
@Tag(name = "Password", description = "Endpoints related to password management (reset, update)")
public class PasswordController {
    @Autowired
    private UserService userService;

    @Autowired
    private PasswordResetTokenService passwordResetTokenService;


    @Autowired
    private EmailService emailService;

    @Value("${server.port}")
    private String serverPort;

/*
    @PostMapping("/forgot")
    public ResponseEntity<String> forgotPassword(@RequestParam("email") String email) {
        Optional<User> userOptional = userService.findByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest().body("User with this email does not exist.");
        }

        User user = userOptional.get();
        String token = passwordResetTokenService.createToken(user).getToken();

//        String resetUrl = "http://localhost:" + serverPort + "/api/password/reset?token=" + token;
        String resetUrl = "http://localhost:4200/reset-password?token=" + token;
        emailService.sendSimpleMessage(
                user.getEmail(),
                "Password Reset Request",
                "Click the link to reset your password: " + resetUrl
        );

        return ResponseEntity.ok("Password reset link sent to your email.");

    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(@RequestParam("token") String token,
                                                @RequestParam("password") String password) {
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenService.validateToken(token);
        if (!tokenOptional.isPresent()) {
            return ResponseEntity.badRequest().body("Invalid or expired token.");
        }

        User user = tokenOptional.get().getUser();
        userService.updatePassword(user, password);

        return ResponseEntity.ok("Password reset successful.");
    }

 */
    /**
     * Step 1: User provides their email to receive an OTP.
     */
    @PostMapping("/forgot")
    public ResponseEntity<String> forgotPassword(@RequestParam("email") String email) {
        Optional<User> userOptional = userService.findByEmail(email);

        // For security, always return a positive message
        if (userOptional.isPresent()) {
            passwordResetTokenService.createAndSendOtp(userOptional.get());
        }

        return ResponseEntity.ok("If an account with that email exists, an OTP has been sent.");
    }



    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam("email") String email, @RequestParam("otp") String otp) {
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            // Return an error with a JSON body
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP or email."));
        }

        if (passwordResetTokenService.validateOtp(userOptional.get(), otp).isPresent()) {
            // --- FIX IS HERE ---
            // Return a success message within a JSON object
            return ResponseEntity.ok(Map.of("message", "OTP verified successfully."));
        }

        // Return an error with a JSON body
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP."));
    }

    /**
     * Step 3: User provides email, the verified OTP, and the new password.
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        Optional<User> userOptional = userService.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request."));
        }

        User user = userOptional.get();

        // Re-validate the OTP one last time
        if (passwordResetTokenService.validateOtp(user, request.getOtp()).isPresent()) {

            // --- FIX IS HERE: Call the single transactional service method ---
            userService.updatePasswordAndInvalidateToken(user, request.getNewPassword());

            return ResponseEntity.ok(Map.of("message", "Password reset successful."));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP."));
    }
}
