package com.thinkerscave.finance.payroll.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class PayrollGenerateRequest {
    @NotNull
    private Integer year;
    @NotNull @Min(1) @Max(12)
    private Integer month;
    private List<Long> staffIds;
    /** Optional LOP day overrides keyed by staffId. */
    private Map<Long, Integer> lopOverrides = new HashMap<>();
}
