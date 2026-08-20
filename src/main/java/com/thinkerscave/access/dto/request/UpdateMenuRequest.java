package com.thinkerscave.access.dto.request;

import com.thinkerscave.access.enums.MenuScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a menu")
public class UpdateMenuRequest {

    @NotBlank(message = "Menu name is required")
    @Size(max = 150)
    private String menuName;

    @Size(max = 500)
    private String description;

    @Size(max = 255)
    private String route;

    @Size(max = 100)
    private String icon;

    private Long parentMenuId;
    private Integer displayOrder;
    private Boolean showInSidebar;
    private Boolean defaultPage;

    @Schema(description = "false = draft, true = saved/live")
    private Boolean active;

    private MenuScope menuScope;
    private Long featureId;
}
