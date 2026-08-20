package com.thinkerscave.access.dto.request;

import com.thinkerscave.access.enums.MenuScope;
import com.thinkerscave.access.enums.MenuType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a menu/page")
public class CreateMenuRequest {

    @NotBlank(message = "Menu code is required")
    @Pattern(regexp = "^[A-Z0-9_]{2,100}$", message = "Menu code must be uppercase alphanumeric with underscores")
    @Schema(example = "STUDENT_ADMISSION")
    private String menuCode;

    @NotBlank(message = "Menu name is required")
    @Size(max = 150)
    @Schema(example = "Student Admission")
    private String menuName;

    @Size(max = 500)
    private String description;

    @Size(max = 255)
    @Schema(description = "Angular route path")
    private String route;

    @Size(max = 100)
    @Schema(description = "PrimeNG icon class", example = "pi pi-users")
    private String icon;

    @NotNull(message = "Menu type is required")
    private MenuType menuType;

    @Schema(description = "Parent menu ID (null for top-level)")
    private Long parentMenuId;

    private Integer displayOrder = 1;
    private Boolean showInSidebar = true;
    private Boolean defaultPage = false;

    @Schema(description = "false = draft (not shown to tenants), true = saved and live")
    private Boolean active = true;

    @Schema(description = "PLATFORM never copied to tenants; CORE always entitled; SUBSCRIPTION gated by feature")
    private MenuScope menuScope;

    @Schema(description = "Feature that unlocks this top-level menu for subscribed tenants")
    private Long featureId;
}
