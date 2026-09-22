package com.thinkerscave.finance.reports.service;

import com.thinkerscave.finance.reports.dto.FinanceReportDtos.ExportFile;
import com.thinkerscave.finance.reports.dto.FinanceReportDtos.Period;

import java.time.LocalDate;

public interface FinanceReportExportService {
    ExportFile export(String format, Long academicYearId, Period period, LocalDate from, LocalDate to, Integer recentLimit);
}
