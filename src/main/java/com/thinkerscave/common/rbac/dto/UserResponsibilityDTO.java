package com.thinkerscave.common.rbac.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponsibilityDTO {
    private Long id;
    private Long userId;
    private Long responsibilityId;
    private Long scopeRefId;
    private Long academicYearId;
    private LocalDate validFrom;
    private LocalDate validTo;
    private boolean active;
}
