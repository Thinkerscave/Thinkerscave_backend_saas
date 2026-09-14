package com.thinkerscave.finance.service.impl;

import com.thinkerscave.finance.entity.StudentBillingPeriod;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.finance.service.PaymentAllocationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAllocationServiceImplTest {

    private final PaymentAllocationServiceImpl service = new PaymentAllocationServiceImpl();

    @Test
    void fifoAllocatesOldestDueFirst() {
        StudentBillingPeriod p1 = period(1L, "2026-04", LocalDate.of(2026, 4, 10), "100.00");
        StudentBillingPeriod p2 = period(2L, "2026-05", LocalDate.of(2026, 5, 10), "100.00");
        PaymentAllocationService.AllocationResult result =
                service.plan(new BigDecimal("150.00"), List.of(p2, p1));

        assertEquals(2, result.allocations().size());
        assertEquals(1L, result.allocations().get(0).periodId());
        assertEquals(new BigDecimal("100.00"), result.allocations().get(0).amount());
        assertEquals(2L, result.allocations().get(1).periodId());
        assertEquals(new BigDecimal("50.00"), result.allocations().get(1).amount());
        assertEquals(new BigDecimal("150.00"), result.allocatedTotal());
        assertEquals(new BigDecimal("0.00"), result.remaining());
    }

    @Test
    void overpayBecomesAdvanceRemaining() {
        StudentBillingPeriod p1 = period(1L, "ONE_TIME", LocalDate.of(2026, 4, 10), "80.00");
        PaymentAllocationService.AllocationResult result =
                service.plan(new BigDecimal("100.00"), List.of(p1));
        assertEquals(new BigDecimal("80.00"), result.allocatedTotal());
        assertEquals(new BigDecimal("20.00"), result.remaining());
    }

    private static StudentBillingPeriod period(Long id, String key, LocalDate due, String balance) {
        StudentBillingPeriod p = new StudentBillingPeriod();
        p.setStudentBillingPeriodId(id);
        p.setPeriodKey(key);
        p.setPeriodLabel(key);
        p.setPeriodStart(due.withDayOfMonth(1));
        p.setDueDate(due);
        p.setBalanceAmount(new BigDecimal(balance));
        p.setPaidAmount(BigDecimal.ZERO);
        p.setTotalAmount(new BigDecimal(balance));
        p.setStatus(BillingPeriodStatus.DUE);
        return p;
    }
}
