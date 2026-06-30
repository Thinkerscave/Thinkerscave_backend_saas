package com.thinkerscave.access.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sidebar tree node for the frontend navigation")
public class SidebarItemResponse {

    private Long id;
    private String menuCode;
    private String menuName;
    private String route;
    private String icon;
    private Integer displayOrder;
    private Boolean defaultPage;

    // Effective permissions for the current user
    private Boolean canView;
    private Boolean canManage;
    private Boolean canApprove;

    private List<SidebarItemResponse> children;
}
