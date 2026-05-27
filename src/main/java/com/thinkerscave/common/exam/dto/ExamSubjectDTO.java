package com.thinkerscave.common.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSubjectDTO {
    private Long id;

    @NotNull
    private Long subjectId;

    @NotNull
    private BigDecimal maxMarks;

    @NotNull
    private BigDecimal passingMarks;

    private Integer weightagePercent;
    private boolean optional;
}
