package com.thinkerscave.finance.dto.request;

import com.thinkerscave.finance.enums.FeeMasterStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    @NotNull
    private FeeMasterStatus status;
}
