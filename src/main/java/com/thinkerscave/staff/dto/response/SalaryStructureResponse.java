package com.thinkerscave.staff.dto.response;

import com.thinkerscave.staff.enums.SalaryType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SalaryStructureResponse {

    private Long salaryStructureId;
    private Long staffId;
    private String staffName;
    private String staffCode;
    private SalaryType salaryType;
    private BigDecimal basicPay;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal specialAllowance;
    private BigDecimal transportAllowance;
    private BigDecimal otherAllowance;
    private BigDecimal pfEmployee;
    private BigDecimal esiEmployee;
    private BigDecimal professionalTax;
    private BigDecimal otherDeduction;
    private BigDecimal totalStatutoryDeductions;
    private BigDecimal grossSalary;
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}
