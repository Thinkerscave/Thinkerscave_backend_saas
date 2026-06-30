package com.thinkerscave.access.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User-level permission overrides — full replace semantics")
public class UpdateUserPermissionsRequest {

    @NotNull(message = "Permission overrides are required")
    private List<PermissionOverride> overrides;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionOverride {

        @NotNull(message = "Menu ID is required")
        private Long menuId;

        private Boolean canView = false;
        private Boolean canManage = false;
        private Boolean canApprove = false;

        @Schema(description = "False = disable this override, use role permission instead")
        private Boolean active = true;
    }
}
