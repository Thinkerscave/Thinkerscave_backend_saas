package com.thinkerscave.platform.dto.request;

import com.thinkerscave.platform.enums.BillingCycle;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SubscriptionChangeRequest {

    @NotNull
    private Long newPlanId;

    private BillingCycle billingCycle;

    private LocalDate effectiveDate;

    @Size(max = 1000)
    private String remarks;
}
