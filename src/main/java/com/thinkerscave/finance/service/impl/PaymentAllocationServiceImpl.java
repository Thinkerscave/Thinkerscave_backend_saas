package com.thinkerscave.finance.service.impl;

import com.thinkerscave.finance.entity.StudentBillingPeriod;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.finance.service.PaymentAllocationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PaymentAllocationServiceImpl implements PaymentAllocationService {

    @Override
    public AllocationResult plan(BigDecimal paymentAmount, List<StudentBillingPeriod> openPeriods) {
        BigDecimal remaining = scale(paymentAmount);
        List<AllocationPlan> plans = new ArrayList<>();
        List<StudentBillingPeriod> sorted = openPeriods.stream()
                .filter(p -> p.getBalanceAmount() != null && p.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator
                        .comparing(StudentBillingPeriod::getDueDate)
                        .thenComparing(StudentBillingPeriod::getPeriodStart)
                        .thenComparing(StudentBillingPeriod::getStudentBillingPeriodId))
                .toList();

        for (StudentBillingPeriod period : sorted) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal take = remaining.min(scale(period.getBalanceAmount()));
            if (take.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            plans.add(new AllocationPlan(
                    period.getStudentBillingPeriodId(),
                    period.getPeriodKey(),
                    period.getPeriodLabel(),
                    take));
            remaining = remaining.subtract(take);
        }
        BigDecimal allocated = scale(paymentAmount).subtract(remaining);
        return new AllocationResult(plans, allocated, remaining);
    }

    @Override
    public void applyStatus(StudentBillingPeriod period, LocalDate today) {
        BigDecimal balance = scale(period.getBalanceAmount());
        BigDecimal paid = scale(period.getPaidAmount());
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            period.setStatus(BillingPeriodStatus.PAID);
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            period.setStatus(BillingPeriodStatus.PARTIALLY_PAID);
        } else if (period.getDueDate() != null && period.getDueDate().isBefore(today)) {
            period.setStatus(BillingPeriodStatus.OVERDUE);
        } else {
            period.setStatus(BillingPeriodStatus.DUE);
        }
    }

    private static BigDecimal scale(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
}
