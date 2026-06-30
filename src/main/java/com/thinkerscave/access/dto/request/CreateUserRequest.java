package com.thinkerscave.access.dto.request;

import com.thinkerscave.access.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new user")
public class CreateUserRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    @Schema(description = "User's first name")
    private String firstName;

    @Size(max = 100)
    @Schema(description = "User's last name")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email required")
    @Size(max = 150)
    @Schema(description = "Login email — must be unique")
    private String email;

    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,100}$", message = "Username must be 3-100 alphanumeric characters")
    @Schema(description = "Login username (auto-generated from email if blank)")
    private String username;

    @Size(min = 10, max = 20, message = "Mobile number must be 10-20 characters")
    @Schema(description = "Mobile number")
    private String mobileNumber;

    @NotNull(message = "Role type is required")
    @Schema(description = "System role for the user")
    private RoleType roleType;

    @Schema(description = "Send welcome email with credentials")
    private Boolean sendWelcomeEmail = true;
}
