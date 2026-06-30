package com.thinkerscave.access.dto.response;

import com.thinkerscave.access.enums.LoginStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login history entry")
public class LoginHistoryResponse {

    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private LoginStatus status;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String ipAddress;
    private String browser;
    private String operatingSystem;
    private String failureReason;
}
