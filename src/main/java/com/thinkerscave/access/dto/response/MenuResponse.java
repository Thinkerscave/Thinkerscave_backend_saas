package com.thinkerscave.access.dto.response;

import com.thinkerscave.access.enums.MenuScope;
import com.thinkerscave.access.enums.MenuType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Menu item response")
public class MenuResponse {

    private Long id;
    private String menuCode;
    private String menuName;
    private String description;
    private String route;
    private String icon;
    private MenuType menuType;
    private Long parentMenuId;
    private String parentMenuName;
    private Integer displayOrder;
    private Boolean showInSidebar;
    private Boolean active;
    private Boolean defaultPage;
    private MenuScope menuScope;
    private Long featureId;
    private String featureCode;
    private String featureName;
    private List<MenuResponse> children;
}
