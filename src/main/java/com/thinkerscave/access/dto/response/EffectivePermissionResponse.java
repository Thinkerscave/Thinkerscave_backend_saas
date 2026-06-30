package com.thinkerscave.access.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Effective permission for a user on a specific menu — merges role + user override")
public class EffectivePermissionResponse {

    private Long menuId;
    private String menuCode;
    private String menuName;
    private Boolean canView;
    private Boolean canManage;
    private Boolean canApprove;

    @Schema(description = "true = from user override, false = from role permission")
    private Boolean isOverride;
}
