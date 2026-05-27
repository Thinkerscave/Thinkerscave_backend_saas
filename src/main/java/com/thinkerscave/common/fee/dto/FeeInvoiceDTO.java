package com.thinkerscave.common.fee.dto;

import com.thinkerscave.common.fee.domain.InvoiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeInvoiceDTO {

    private Long id;
    private String invoiceNumber;
    private Long feeContractId;
    private Long studentId;
    private Long enrollmentId;
    private Long academicYearId;
    private String periodLabel;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal taxTotal;
    private BigDecimal lateFeeTotal;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private InvoiceStatus status;
    private String notes;
    private List<FeeInvoiceLineDTO> lines;
}
