package com.thinkerscave.finance.payroll.dto.request;

import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    @NotNull
    private ComponentStatus status;
}
