package com.thinkerscave.common.fee.dto;

import com.thinkerscave.common.fee.domain.FeeFrequency;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeStructureItemDTO {

    private Long id;

    @NotNull
    private Long feeHeadId;

    private Long feeGroupId;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private FeeFrequency frequency;

    private Integer dueDayOfMonth;

    private boolean optional;

    private Integer displayOrder;
}
