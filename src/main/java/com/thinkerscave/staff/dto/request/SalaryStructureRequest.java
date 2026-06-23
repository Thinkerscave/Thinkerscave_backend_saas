package com.thinkerscave.staff.dto.request;

import com.thinkerscave.staff.enums.SalaryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SalaryStructureRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    @NotNull(message = "Salary type is required")
    private SalaryType salaryType;

    private BigDecimal basicPay;

    private BigDecimal hra;

    private BigDecimal da;

    private BigDecimal specialAllowance;

    private BigDecimal transportAllowance;

    private BigDecimal otherAllowance;

    @Size(max = 150)
    private String bankName;

    @Size(max = 150)
    private String accountHolderName;

    @Size(max = 50)
    private String accountNumber;

    @Size(max = 20)
    private String ifscCode;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;
}
