package com.thinkerscave.finance.service.impl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Documents the closed Student Fee KPI formulas from the finalized clarifications.
 */
class StudentFeeKpiFormulaTest {

    @Test
    void advanceIsPaymentsMinusAllocationsNeverNegative() {
        BigDecimal payments = new BigDecimal("30000.00");
        BigDecimal allocations = new BigDecimal("20000.00");
        BigDecimal advance = payments.subtract(allocations).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("10000.00"), advance);

        BigDecimal overAllocated = new BigDecimal("100.00").subtract(new BigDecimal("150.00"))
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("0.00"), overAllocated);
    }

    @Test
    void studentKpisUsePeriodTotalsPaidAndBalances() {
        BigDecimal totalFee = new BigDecimal("10000").add(new BigDecimal("10000")); // Σ period totals
        BigDecimal paid = new BigDecimal("10000").add(new BigDecimal("5000"));       // Σ period paid
        BigDecimal outstanding = new BigDecimal("0").add(new BigDecimal("5000"));    // Σ period balances
        assertEquals(new BigDecimal("20000"), totalFee);
        assertEquals(new BigDecimal("15000"), paid);
        assertEquals(new BigDecimal("5000"), outstanding);
    }
}
