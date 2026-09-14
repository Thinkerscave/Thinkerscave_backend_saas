package com.thinkerscave.finance.reports.service;

import com.thinkerscave.finance.reports.dto.FinanceReportDtos.Overview;
import com.thinkerscave.finance.reports.dto.FinanceReportDtos.Period;

import java.time.LocalDate;

public interface FinanceReportService {
    Overview overview(Long academicYearId, Period period, LocalDate from, LocalDate to, Integer recentLimit);
}
