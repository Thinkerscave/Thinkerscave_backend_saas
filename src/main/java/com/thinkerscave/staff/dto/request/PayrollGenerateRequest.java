package com.thinkerscave.staff.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayrollGenerateRequest {

    @NotNull(message = "Year is required")
    @Min(2000)
    @Max(2100)
    private Integer year;

    @NotNull(message = "Month is required")
    @Min(1)
    @Max(12)
    private Integer month;
}
