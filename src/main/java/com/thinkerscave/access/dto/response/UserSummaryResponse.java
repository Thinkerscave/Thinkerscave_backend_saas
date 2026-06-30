package com.thinkerscave.access.dto.response;

import com.thinkerscave.access.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User summary for lists and search results")
public class UserSummaryResponse {

    private Long id;
    private String userCode;
    private String username;
    private String email;
    private String mobileNumber;
    private String firstName;
    private String lastName;
    private String displayName;
    private String profileImageUrl;
    private UserStatus status;
    private Boolean accountLocked;
    private Boolean firstTimeLogin;
    private Boolean emailVerified;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdOn;
    private List<UserRoleSummary> roles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRoleSummary {
        private Long roleId;
        private String roleName;
        private String roleCode;
        private String roleType;
        private Boolean primaryRole;
    }
}
