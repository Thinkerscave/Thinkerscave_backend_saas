package com.thinkerscave.common.payroll.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PayrollDTO {
    private Long id;
    private Long organizationId;
    private Long staffId;
    private String staffName;
    private String department;
    private String designation;

    // Earnings
    private BigDecimal basic;
    private BigDecimal hra;
    private BigDecimal specialAllowance;
    private BigDecimal academicAllowance;
    private BigDecimal medicalAllowance;
    private BigDecimal travelAllowance;
    private BigDecimal dearnessAllowance;
    private BigDecimal otherAllowance;

    // Deductions
    private BigDecimal professionalTax;
    private BigDecimal incomeTax;
    private BigDecimal providentFund;

    // Computed
    private BigDecimal grossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private BigDecimal ctcAnnual;

    private LocalDate effectiveFrom;
}
