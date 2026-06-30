package com.thinkerscave.access.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a user's profile")
public class UpdateUserRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(min = 10, max = 20)
    private String mobileNumber;

    @Size(max = 200)
    private String displayName;

    @Size(max = 500)
    private String profileImageUrl;
}
