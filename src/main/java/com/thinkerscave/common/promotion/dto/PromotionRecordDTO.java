package com.thinkerscave.common.promotion.dto;

import com.thinkerscave.common.promotion.domain.PromotionDecision;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRecordDTO {
    private Long id;
    private Long promotionBatchId;
    private Long studentId;
    private Long fromEnrollmentId;
    private Long toEnrollmentId;
    private PromotionDecision decision;
    private String reason;
}
