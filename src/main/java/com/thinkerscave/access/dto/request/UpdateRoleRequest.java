package com.thinkerscave.access.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a role")
public class UpdateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 100)
    private String roleName;

    @Size(max = 500)
    private String description;

    @Size(max = 50)
    private String dashboardCode;

    private Integer displayOrder;
}
