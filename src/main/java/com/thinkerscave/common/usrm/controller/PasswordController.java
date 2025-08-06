package com.thinkerscave.common.usrm.controller;

import com.thinkerscave.common.usrm.domain.PasswordResetToken;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.service.PasswordResetTokenService;
import com.thinkerscave.common.usrm.service.UserService;
import com.thinkerscave.common.usrm.service.impl.EmailService;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


    @PostMapping("/forgot")
    public ResponseEntity<String> forgotPassword(@RequestParam("email") String email) {
        Optional<User> userOptional = userService.findByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest().body("User with this email does not exist.");
        }

        User user = userOptional.get();
        String token = passwordResetTokenService.createToken(user).getToken();

        String resetUrl = "http://localhost:" + serverPort + "/api/password/reset?token=" + token;

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
}
