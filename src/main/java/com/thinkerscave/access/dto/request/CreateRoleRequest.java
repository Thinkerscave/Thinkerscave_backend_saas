package com.thinkerscave.access.dto.request;

import com.thinkerscave.access.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a role")
public class CreateRoleRequest {

    @NotBlank(message = "Role code is required")
    @Pattern(regexp = "^ROLE_[A-Z0-9_]{1,40}$", message = "Role code must start with ROLE_ and contain uppercase letters, digits, underscores")
    @Schema(example = "ROLE_STAFF")
    private String roleCode;

    @NotBlank(message = "Role name is required")
    @Size(max = 100)
    @Schema(example = "Staff Member")
    private String roleName;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Role type is required")
    private RoleType roleType;

    @Size(max = 50)
    @Schema(description = "Dashboard identifier (e.g., STAFF, STUDENT, ADMIN)")
    private String dashboardCode;

    private Integer displayOrder = 1;

    @Schema(description = "false = draft, true = saved")
    private Boolean active = true;
}
