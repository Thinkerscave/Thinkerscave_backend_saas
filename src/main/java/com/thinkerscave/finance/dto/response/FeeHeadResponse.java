package com.thinkerscave.finance.dto.response;

import com.thinkerscave.finance.enums.FeeHeadCategory;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeeHeadResponse {
    private Long feeHeadId;
    private String name;
    private FeeHeadCategory category;
    private String description;
    private FeeMasterStatus status;
}
