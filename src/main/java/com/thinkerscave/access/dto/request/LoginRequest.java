package com.thinkerscave.access.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request")
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Schema(description = "Username or email")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password")
    private String password;

    @Schema(description = "Device name for session tracking")
    private String deviceName;

    @Schema(description = "Remember this device")
    private Boolean rememberMe = false;
}
