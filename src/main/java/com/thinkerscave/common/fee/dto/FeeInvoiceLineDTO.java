package com.thinkerscave.common.fee.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeInvoiceLineDTO {

    private Long id;
    private Long feeHeadId;
    private String description;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
    private Integer displayOrder;
}
