package com.thinkerscave.common.fee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeePolicyDTO {

    private Long id;

    @NotBlank @Size(max = 64)
    private String code;

    @NotBlank @Size(max = 128)
    private String name;

    private Integer gracePeriodDays;
    private BigDecimal lateFeeAmount;
    private BigDecimal lateFeePercent;
    private Integer compoundingDays;
    private BigDecimal maxLateFee;
    private BigDecimal earlyBirdDiscountPercent;
    private Integer earlyBirdCutoffDays;

    /** Comma-separated days before/after due date for reminder dispatch, e.g. "7,3,0,-3,-7". */
    @Size(max = 128)
    private String reminderIntervalsCsv;

    private boolean active;
}
