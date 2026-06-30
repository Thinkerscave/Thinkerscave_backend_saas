package com.thinkerscave.access.dto.response;

import com.thinkerscave.access.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role summary")
public class RoleResponse {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private RoleType roleType;
    private String dashboardCode;
    private Boolean systemRole;
    private Boolean active;
    private Integer displayOrder;
    private Long activeUserCount;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}
