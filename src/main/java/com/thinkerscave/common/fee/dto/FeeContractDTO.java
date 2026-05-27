package com.thinkerscave.common.fee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeContractDTO {

    private Long id;

    @NotNull
    private Long enrollmentId;

    @NotNull
    private Long studentId;

    @NotNull
    private Long feeStructureId;

    @NotNull
    private Long academicYearId;

    private BigDecimal annualAmount;
    private BigDecimal discountAmount;
    private BigDecimal scholarshipAmount;
    private BigDecimal netPayable;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 500)
    private String remarks;
}
