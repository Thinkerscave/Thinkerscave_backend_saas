package com.thinkerscave.access.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Full permission matrix for a role — one row per menu")
public class PermissionMatrixResponse {

    private Long roleId;
    private String roleCode;
    private String roleName;
    private Long organizationId;
    private List<PermissionRow> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionRow {
        private Long menuId;
        private String menuCode;
        private String menuName;
        private String menuType;
        private Long parentMenuId;
        private String parentMenuName;
        private Boolean canView;
        private Boolean canManage;
        private Boolean canApprove;
    }
}
