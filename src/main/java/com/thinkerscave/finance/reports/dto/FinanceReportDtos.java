package com.thinkerscave.finance.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class FinanceReportDtos {
    private FinanceReportDtos() {}

    public enum Period {
        THIS_MONTH, LAST_MONTH, THIS_QUARTER, THIS_ACADEMIC_YEAR, THIS_FINANCIAL_YEAR, CUSTOM
    }

    public record Filter(Long academicYearId, String academicYearName, Period period, String label,
                         LocalDate from, LocalDate to, String outstandingAsOf) {}
    public record Kpis(BigDecimal feeCollection, BigDecimal outstandingFees, BigDecimal payrollPaid,
                       BigDecimal expensesPaid, BigDecimal totalOutflow, BigDecimal netOperationalFlow) {}
    public record TrendPoint(String bucket, String label, BigDecimal feeCollection,
                             BigDecimal totalOutflow, BigDecimal difference) {}
    public record MonthlyAmount(String bucket, String label, BigDecimal amount) {}
    public record AmountPercentage(String name, BigDecimal amount, BigDecimal percentage) {}
    public record FeeAnalytics(List<MonthlyAmount> collectionTrend, List<AmountPercentage> collectionByCategory,
                               String attributionMethod) {}
    public record OutstandingClass(Long classId, String className, long studentsDue, BigDecimal outstandingAmount) {}
    public record Outstanding(BigDecimal amount, long studentsDue, List<OutstandingClass> byClass) {}
    public record Payroll(BigDecimal generatedAmount, BigDecimal paidAmount, BigDecimal pendingAmount,
                          long employeeCount, List<MonthlyAmount> costTrend) {}
    public record StatusAmount(String status, long count, BigDecimal amount) {}
    public record ExpenseHead(Long headId, String headName, String categoryName,
                              BigDecimal amount, BigDecimal percentage, String drillPath) {}
    public record Expenses(BigDecimal paidAmount, BigDecimal pendingApprovalAmount, long pendingApprovalCount,
                           BigDecimal approvedUnpaidAmount, long approvedUnpaidCount,
                           List<AmountPercentage> byCategory, List<StatusAmount> statusDistribution,
                           List<ExpenseHead> topHeads) {}
    public record AttentionItem(String type, String label, BigDecimal amount, long count, String drillPath) {}
    public record RecentActivity(String type, LocalDate date, String reference, String description,
                                 BigDecimal amount, String status, String drillPath) {}
    public record Overview(Filter filter, Kpis kpis, List<TrendPoint> incomeOutflowTrend,
                           FeeAnalytics feeAnalytics, Outstanding outstanding, Payroll payroll,
                           Expenses expenses, List<AttentionItem> attention,
                           List<RecentActivity> recentActivity) {}
    public record ExportFile(byte[] content, String contentType, String fileName) {}
}
