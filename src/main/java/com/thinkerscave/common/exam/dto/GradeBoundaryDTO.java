package com.thinkerscave.common.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeBoundaryDTO {
    private Long id;

    @NotBlank
    private String gradeCode;

    private String gradeLabel;

    @NotNull
    private BigDecimal minPercent;

    @NotNull
    private BigDecimal maxPercent;

    private BigDecimal gradePoint;
    private boolean pass;
    private Integer displayOrder;
}
