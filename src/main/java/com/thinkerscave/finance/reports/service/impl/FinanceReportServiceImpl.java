package com.thinkerscave.finance.reports.service.impl;

import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.finance.reports.dto.FinanceReportDtos.*;
import com.thinkerscave.finance.reports.repository.FinanceReportQueryRepository;
import com.thinkerscave.finance.reports.security.FinanceReportsAccessGuard;
import com.thinkerscave.finance.reports.service.FinanceReportService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceReportServiceImpl implements FinanceReportService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final AcademicYearRepository academicYearRepository;
    private final FinanceReportQueryRepository queries;
    private final FinanceReportsAccessGuard accessGuard;

    @Override
    public Overview overview(Long academicYearId, Period period, LocalDate from, LocalDate to, Integer recentLimit) {
        accessGuard.requireView();
        AcademicYear year = academicYearId == null
                ? academicYearRepository.findByCurrentYearTrue()
                    .orElseThrow(() -> new ResourceNotFoundException("Current academic year not configured"))
                : academicYearRepository.findById(academicYearId)
                    .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        Period selectedPeriod = period == null ? Period.THIS_ACADEMIC_YEAR : period;
        ResolvedPeriod resolved = resolvePeriod(selectedPeriod, from, to, year.getStartDate(), year.getEndDate(), LocalDate.now());
        int limit = recentLimit == null ? 15 : Math.max(1, Math.min(50, recentLimit));

        Object[] rawKpis = queries.kpis(resolved.from(), resolved.to(), year.getAcademicYearId());
        BigDecimal fees = money(rawKpis[0]);
        BigDecimal outstandingAmount = money(rawKpis[1]);
        BigDecimal payrollPaid = money(rawKpis[2]);
        BigDecimal expensesPaid = money(rawKpis[3]);
        BigDecimal outflow = payrollPaid.add(expensesPaid);
        Kpis kpis = new Kpis(fees, outstandingAmount, payrollPaid, expensesPaid, outflow, fees.subtract(outflow));

        List<TrendPoint> trend = monthlyTrend(resolved.from(), resolved.to(), queries.trend(resolved.from(), resolved.to()));
        List<AmountPercentage> feeCategories = percentages(queries.feeCategories(resolved.from(), resolved.to()));
        FeeAnalytics feeAnalytics = new FeeAnalytics(
                trend.stream().map(t -> new MonthlyAmount(t.bucket(), t.label(), t.feeCollection())).toList(),
                feeCategories, "PROPORTIONAL_BY_PERIOD_LINES");

        Object[] os = queries.outstandingSummary(year.getAcademicYearId());
        List<OutstandingClass> byClass = queries.outstandingByClass(year.getAcademicYearId()).stream()
                .map(r -> new OutstandingClass(longValueOrNull(r[0]), string(r[1]), count(r[2]), money(r[3]))).toList();
        Outstanding outstanding = new Outstanding(money(os[0]), count(os[1]), byClass);

        Object[] ps = queries.payrollSummary(resolved.from(), resolved.to());
        Payroll payroll = new Payroll(money(ps[0]), money(ps[1]), money(ps[2]), count(ps[3]),
                monthlySingleTrend(resolved.from(), resolved.to(), queries.payrollTrend(resolved.from(), resolved.to())));

        Object[] ea = queries.expenseAttention();
        List<AmountPercentage> expenseCategories = percentages(queries.expenseCategories(resolved.from(), resolved.to()));
        List<StatusAmount> statuses = queries.expenseStatuses(resolved.from(), resolved.to()).stream()
                .map(r -> new StatusAmount(string(r[0]), count(r[1]), money(r[2]))).toList();
        List<ExpenseHead> heads = topHeads(queries.topExpenseHeads(resolved.from(), resolved.to()), expensesPaid);
        Expenses expenses = new Expenses(expensesPaid, money(ea[0]), count(ea[1]), money(ea[2]), count(ea[3]),
                expenseCategories, statuses, heads);

        List<AttentionItem> attention = attention(outstanding, payroll, expenses);
        List<RecentActivity> recent = queries.recent(resolved.from(), resolved.to(), limit).stream()
                .map(this::recent).toList();
        String label = selectedPeriod == Period.THIS_FINANCIAL_YEAR
                ? "This Financial Year (same range as academic year)"
                : periodLabel(selectedPeriod);
        Filter filter = new Filter(year.getAcademicYearId(), year.getName(), selectedPeriod, label,
                resolved.from(), resolved.to(), "CURRENT");
        return new Overview(filter, kpis, trend, feeAnalytics, outstanding, payroll, expenses, attention, recent);
    }

    public static ResolvedPeriod resolvePeriod(Period period, LocalDate customFrom, LocalDate customTo,
                                                LocalDate academicFrom, LocalDate academicTo, LocalDate today) {
        if (period == null) period = Period.THIS_ACADEMIC_YEAR;
        return switch (period) {
            case THIS_MONTH -> range(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
            case LAST_MONTH -> {
                LocalDate prior = today.minusMonths(1);
                yield range(prior.withDayOfMonth(1), prior.withDayOfMonth(prior.lengthOfMonth()));
            }
            case THIS_QUARTER -> {
                int firstMonth = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate start = LocalDate.of(today.getYear(), firstMonth, 1);
                yield range(start, start.plusMonths(3).minusDays(1));
            }
            case THIS_ACADEMIC_YEAR, THIS_FINANCIAL_YEAR -> range(academicFrom, academicTo);
            case CUSTOM -> {
                if (customFrom == null || customTo == null) {
                    throw new BadRequestException("CUSTOM period requires from and to");
                }
                yield range(customFrom, customTo);
            }
        };
    }

    private static ResolvedPeriod range(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BadRequestException("Report from date must be on or before to date");
        }
        return new ResolvedPeriod(from, to);
    }

    public static Map<String, BigDecimal> proportionalCategorySplit(BigDecimal allocatedAmount,
                                                                     Map<String, BigDecimal> lineAmounts) {
        BigDecimal total = lineAmounts.values().stream().filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allocatedAmount == null || total.signum() == 0) return Map.of();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        lineAmounts.forEach((category, amount) -> {
            if (amount != null && amount.signum() > 0) {
                result.put(category, allocatedAmount.multiply(amount).divide(total, 8, RoundingMode.HALF_UP));
            }
        });
        return result;
    }

    public static Kpis calculateKpis(BigDecimal fees, BigDecimal outstanding, BigDecimal payroll, BigDecimal expenses) {
        fees = nullableMoney(fees); outstanding = nullableMoney(outstanding);
        payroll = nullableMoney(payroll); expenses = nullableMoney(expenses);
        BigDecimal outflow = payroll.add(expenses);
        return new Kpis(fees, outstanding, payroll, expenses, outflow, fees.subtract(outflow));
    }

    private static List<TrendPoint> monthlyTrend(LocalDate from, LocalDate to, List<Object[]> rows) {
        Map<YearMonth, Object[]> values = new HashMap<>();
        rows.forEach(r -> values.put(YearMonth.from(date(r[0])), r));
        List<TrendPoint> result = new ArrayList<>();
        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
            Object[] row = values.get(month);
            BigDecimal fee = row == null ? ZERO : money(row[1]);
            BigDecimal outflow = row == null ? ZERO : money(row[2]);
            result.add(new TrendPoint(month.toString(), month.format(MONTH_LABEL), fee, outflow, fee.subtract(outflow)));
        }
        return result;
    }

    private static List<MonthlyAmount> monthlySingleTrend(LocalDate from, LocalDate to, List<Object[]> rows) {
        Map<YearMonth, BigDecimal> values = new HashMap<>();
        rows.forEach(r -> values.put(YearMonth.from(date(r[0])), money(r[1])));
        List<MonthlyAmount> result = new ArrayList<>();
        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
            BigDecimal amount = values.getOrDefault(month, ZERO);
            result.add(new MonthlyAmount(month.toString(), month.format(MONTH_LABEL), amount));
        }
        return result;
    }

    private static List<AmountPercentage> percentages(List<Object[]> rows) {
        BigDecimal total = rows.stream().map(r -> money(r[1])).reduce(BigDecimal.ZERO, BigDecimal::add);
        return rows.stream().map(r -> new AmountPercentage(string(r[0]), money(r[1]), percentage(money(r[1]), total))).toList();
    }

    private static List<ExpenseHead> topHeads(List<Object[]> rows, BigDecimal total) {
        return rows.stream().map(r -> new ExpenseHead(longValueOrNull(r[0]), string(r[1]), string(r[2]),
                money(r[3]), percentage(money(r[3]), total), "/app/expenses?expenseHeadId=" + r[0])).toList();
    }

    private static List<AttentionItem> attention(Outstanding o, Payroll p, Expenses e) {
        List<AttentionItem> items = new ArrayList<>();
        if (o.amount().signum() > 0) items.add(new AttentionItem("OUTSTANDING_FEES", "Student fees outstanding",
                o.amount(), o.studentsDue(), "/app/fees/students"));
        if (p.pendingAmount().signum() > 0) items.add(new AttentionItem("PAYROLL_PENDING", "Payroll pending payment",
                p.pendingAmount(), p.employeeCount(), "/app/payroll"));
        if (e.pendingApprovalAmount().signum() > 0 || e.pendingApprovalCount() > 0)
            items.add(new AttentionItem("EXPENSE_PENDING_APPROVAL", "Expenses pending approval",
                    e.pendingApprovalAmount(), e.pendingApprovalCount(), "/app/expenses?approvalStatus=PENDING_APPROVAL"));
        if (e.approvedUnpaidAmount().signum() > 0 || e.approvedUnpaidCount() > 0)
            items.add(new AttentionItem("EXPENSE_APPROVED_UNPAID", "Approved expenses pending payment",
                    e.approvedUnpaidAmount(), e.approvedUnpaidCount(),
                    "/app/expenses?approvalStatus=APPROVED&paymentStatus=UNPAID"));
        return items;
    }

    private RecentActivity recent(Object[] r) {
        String type = string(r[0]);
        String drill = switch (type) {
            case "EXPENSE" -> "/app/expenses/" + r[6];
            case "PAYROLL" -> "/app/payroll";
            default -> "/app/fees/receipts";
        };
        return new RecentActivity(type, date(r[1]), string(r[2]), string(r[3]), money(r[4]), string(r[5]), drill);
    }

    private static String periodLabel(Period period) {
        return Arrays.stream(period.name().toLowerCase(Locale.ROOT).split("_"))
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1)).reduce((a, b) -> a + " " + b).orElse("");
    }

    private static BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        return total.signum() == 0 ? ZERO : amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }
    private static BigDecimal money(Object value) { return nullableMoney(value == null ? null : new BigDecimal(value.toString())); }
    private static BigDecimal nullableMoney(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }
    private static long count(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
    private static Long longValueOrNull(Object value) { return value == null ? null : ((Number) value).longValue(); }
    private static String string(Object value) { return value == null ? null : value.toString(); }
    private static LocalDate date(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof Date sqlDate) return sqlDate.toLocalDate();
        return LocalDate.parse(value.toString().substring(0, 10));
    }

    public record ResolvedPeriod(LocalDate from, LocalDate to) {}
}
