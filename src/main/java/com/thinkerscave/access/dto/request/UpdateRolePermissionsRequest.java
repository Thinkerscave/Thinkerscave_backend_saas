package com.thinkerscave.access.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Permission matrix update for a role — full replace semantics")
public class UpdateRolePermissionsRequest {

    @NotNull(message = "Permission rows are required")
    private List<PermissionRow> permissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionRow {

        @NotNull(message = "Menu ID is required")
        private Long menuId;

        private Boolean canView = false;
        private Boolean canManage = false;
        private Boolean canApprove = false;
    }
}
