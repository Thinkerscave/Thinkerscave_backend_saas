package com.thinkerscave.security.dto.response;

import com.thinkerscave.security.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Active user session details")
public class SessionResponse {

    private Long id;
    private Long userId;
    private String username;
    private String deviceName;
    private String browser;
    private String operatingSystem;
    private String ipAddress;
    private SessionStatus status;
    private LocalDateTime loginAt;
    private LocalDateTime logoutAt;
}
