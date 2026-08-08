package com.thinkerscave.student.dto.request;

import com.thinkerscave.student.enums.PromotionDecision;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionRecordUpdateRequest {

    private PromotionDecision decision;
    private Long toClassId;
    private String reason;
}
