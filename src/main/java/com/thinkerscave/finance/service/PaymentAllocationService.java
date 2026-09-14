package com.thinkerscave.finance.service;

import com.thinkerscave.finance.entity.StudentBillingPeriod;
import com.thinkerscave.finance.enums.BillingPeriodStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared FIFO allocation against open billing periods.
 */
public interface PaymentAllocationService {

    record AllocationPlan(Long periodId, String periodKey, String periodLabel, BigDecimal amount) {}

    record AllocationResult(List<AllocationPlan> allocations, BigDecimal allocatedTotal, BigDecimal remaining) {}

    AllocationResult plan(BigDecimal paymentAmount, List<StudentBillingPeriod> openPeriods);

    void applyStatus(StudentBillingPeriod period, LocalDate today);
}
