package com.thinkerscave.common.promotion.dto;

import com.thinkerscave.common.promotion.domain.PromotionStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionBatchDTO {
    private Long id;
    private String batchCode;
    private Long fromAcademicYearId;
    private Long toAcademicYearId;
    private Long fromClassId;
    private Long toClassId;
    private PromotionStatus status;
    private Integer plannedCount;
    private Integer processedCount;
    private LocalDate executedOn;
    private String remarks;
}
