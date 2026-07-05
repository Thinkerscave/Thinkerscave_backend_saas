package com.thinkerscave.security.dto.response;

import com.thinkerscave.access.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response with tokens")
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserSummaryResponse user;
    private String tenantId;
    private String loginContext;
    private Boolean firstTimeLogin;
    private Boolean requirePasswordChange;
}
