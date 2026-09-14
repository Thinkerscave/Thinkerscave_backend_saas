package com.thinkerscave.finance.reports.service.impl;

import com.thinkerscave.finance.reports.dto.FinanceReportDtos.Kpis;
import com.thinkerscave.finance.reports.dto.FinanceReportDtos.Period;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinanceReportServiceImplTest {
    @Test
    void calculatesKpisWithDecimalSafeOutflowAndNetFlow() {
        Kpis kpis = FinanceReportServiceImpl.calculateKpis(
                new BigDecimal("1250.25"), new BigDecimal("800.00"),
                new BigDecimal("300.10"), new BigDecimal("149.90"));
        assertEquals(new BigDecimal("450.00"), kpis.totalOutflow());
        assertEquals(new BigDecimal("800.25"), kpis.netOperationalFlow());
    }

    @Test
    void resolvesPeriodsAndFinancialYearToAcademicRange() {
        LocalDate today = LocalDate.of(2026, 9, 14);
        var month = FinanceReportServiceImpl.resolvePeriod(Period.THIS_MONTH, null, null,
                LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), today);
        assertEquals(LocalDate.of(2026, 9, 1), month.from());
        assertEquals(LocalDate.of(2026, 9, 30), month.to());

        var financial = FinanceReportServiceImpl.resolvePeriod(Period.THIS_FINANCIAL_YEAR, null, null,
                LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), today);
        assertEquals(LocalDate.of(2026, 4, 1), financial.from());
        assertEquals(LocalDate.of(2027, 3, 31), financial.to());
    }

    @Test
    void splitsAllocationProportionallyAcrossPeriodLines() {
        Map<String, BigDecimal> lines = new LinkedHashMap<>();
        lines.put("TUITION", new BigDecimal("600"));
        lines.put("TRANSPORT", new BigDecimal("400"));
        Map<String, BigDecimal> split = FinanceReportServiceImpl.proportionalCategorySplit(
                new BigDecimal("250"), lines);
        assertEquals(0, new BigDecimal("150").compareTo(split.get("TUITION")));
        assertEquals(0, new BigDecimal("100").compareTo(split.get("TRANSPORT")));
    }
}
