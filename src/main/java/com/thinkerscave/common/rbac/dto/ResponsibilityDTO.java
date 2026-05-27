package com.thinkerscave.common.rbac.dto;

import com.thinkerscave.common.enums.GenericStatus;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponsibilityDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String scopeType;
    private GenericStatus status;
    /** Privilege IDs granted by this responsibility. */
    @Builder.Default
    private List<Long> privilegeIds = new ArrayList<>();
}
