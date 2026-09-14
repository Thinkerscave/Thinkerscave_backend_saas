package com.thinkerscave.finance.reports.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.thinkerscave.finance.reports.dto.FinanceReportDtos.*;
import com.thinkerscave.finance.reports.service.FinanceReportExportService;
import com.thinkerscave.finance.reports.service.FinanceReportService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FinanceReportExportServiceImpl implements FinanceReportExportService {
    private final FinanceReportService reportService;

    @Override
    public ExportFile export(String format, Long academicYearId, Period period, LocalDate from, LocalDate to,
                             Integer recentLimit) {
        String normalized = format == null ? "" : format.toLowerCase(Locale.ROOT);
        if (!normalized.equals("pdf") && !normalized.equals("csv")) {
            throw new BadRequestException("format must be pdf or csv");
        }
        Overview report = reportService.overview(academicYearId, period, from, to, recentLimit);
        String stem = "finance-report-" + report.filter().from() + "-to-" + report.filter().to();
        return normalized.equals("pdf")
                ? new ExportFile(pdf(report), "application/pdf", stem + ".pdf")
                : new ExportFile(csv(report), "text/csv; charset=UTF-8", stem + ".csv");
    }

    private byte[] csv(Overview r) {
        StringBuilder out = new StringBuilder("\uFEFF");
        out.append("Finance Reports\n");
        out.append("Academic Year,").append(cell(r.filter().academicYearName())).append('\n');
        out.append("Period,").append(cell(r.filter().label())).append('\n');
        out.append("From,").append(r.filter().from()).append(",To,").append(r.filter().to()).append("\n\n");
        out.append("KPI,Amount\n");
        kpi(out, "Fee Collection", r.kpis().feeCollection());
        kpi(out, "Outstanding Fees", r.kpis().outstandingFees());
        kpi(out, "Payroll Paid", r.kpis().payrollPaid());
        kpi(out, "Expenses Paid", r.kpis().expensesPaid());
        kpi(out, "Total Outflow", r.kpis().totalOutflow());
        kpi(out, "Net Operational Flow", r.kpis().netOperationalFlow());
        out.append("\nMonth,Fee Collection,Total Outflow,Difference\n");
        r.incomeOutflowTrend().forEach(x -> out.append(cell(x.label())).append(',').append(x.feeCollection())
                .append(',').append(x.totalOutflow()).append(',').append(x.difference()).append('\n'));
        out.append("\nFee Category,Amount,Percentage\n");
        r.feeAnalytics().collectionByCategory().forEach(x -> out.append(cell(x.name())).append(',')
                .append(x.amount()).append(',').append(x.percentage()).append('\n'));
        out.append("\nOutstanding Class,Students Due,Amount\n");
        r.outstanding().byClass().forEach(x -> out.append(cell(x.className())).append(',')
                .append(x.studentsDue()).append(',').append(x.outstandingAmount()).append('\n'));
        out.append("\nPayroll,Amount\n");
        kpi(out, "Generated", r.payroll().generatedAmount());
        kpi(out, "Paid", r.payroll().paidAmount());
        kpi(out, "Pending", r.payroll().pendingAmount());
        out.append("Employees,").append(r.payroll().employeeCount()).append('\n');
        out.append("\nExpense Category,Amount,Percentage\n");
        r.expenses().byCategory().forEach(x -> out.append(cell(x.name())).append(',')
                .append(x.amount()).append(',').append(x.percentage()).append('\n'));
        out.append("\nExpense Head,Category,Amount,Percentage\n");
        r.expenses().topHeads().forEach(x -> out.append(cell(x.headName())).append(',')
                .append(cell(x.categoryName())).append(',').append(x.amount()).append(',')
                .append(x.percentage()).append('\n'));
        out.append("\nNeeds Attention,Amount,Count\n");
        r.attention().forEach(x -> out.append(cell(x.label())).append(',').append(x.amount())
                .append(',').append(x.count()).append('\n'));
        out.append("\nRecent Type,Date,Reference,Description,Amount,Status\n");
        r.recentActivity().forEach(x -> out.append(cell(x.type())).append(',').append(x.date()).append(',')
                .append(cell(x.reference())).append(',').append(cell(x.description())).append(',')
                .append(x.amount()).append(',').append(cell(x.status())).append('\n'));
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(Overview r) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("FINANCE REPORTS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph(r.filter().academicYearName() + " | " + r.filter().label()
                    + " | " + r.filter().from() + " to " + r.filter().to()));
            document.add(new Paragraph("Management operational report; not a statutory financial statement.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8)));
            document.add(Chunk.NEWLINE);
            PdfPTable kpis = table("KPI", "Amount");
            add(kpis, "Fee Collection", r.kpis().feeCollection());
            add(kpis, "Outstanding Fees (current)", r.kpis().outstandingFees());
            add(kpis, "Payroll Paid", r.kpis().payrollPaid());
            add(kpis, "Expenses Paid", r.kpis().expensesPaid());
            add(kpis, "Total Outflow", r.kpis().totalOutflow());
            add(kpis, "Net Operational Flow", r.kpis().netOperationalFlow());
            document.add(kpis);
            document.add(Chunk.NEWLINE);
            PdfPTable trend = table("Month", "Fee Collection", "Total Outflow", "Difference");
            r.incomeOutflowTrend().forEach(x -> add(trend, x.label(), x.feeCollection(), x.totalOutflow(), x.difference()));
            document.add(trend);
            document.add(Chunk.NEWLINE);
            PdfPTable categories = table("Fee Category", "Amount", "Percentage");
            r.feeAnalytics().collectionByCategory().forEach(x -> add(categories, x.name(), x.amount(), x.percentage() + "%"));
            document.add(categories);
            document.add(Chunk.NEWLINE);
            PdfPTable operations = table("Operational Summary", "Amount / Count");
            add(operations, "Payroll generated", r.payroll().generatedAmount());
            add(operations, "Payroll paid", r.payroll().paidAmount());
            add(operations, "Payroll pending", r.payroll().pendingAmount());
            add(operations, "Employees", r.payroll().employeeCount());
            add(operations, "Expenses pending approval", r.expenses().pendingApprovalAmount());
            add(operations, "Approved expenses unpaid", r.expenses().approvedUnpaidAmount());
            document.add(operations);
            document.add(Chunk.NEWLINE);
            PdfPTable heads = table("Expense Head", "Category", "Paid Amount", "Percentage");
            r.expenses().topHeads().forEach(x -> add(heads, x.headName(), x.categoryName(), x.amount(), x.percentage() + "%"));
            document.add(heads);
            document.add(Chunk.NEWLINE);
            PdfPTable recent = table("Type", "Date", "Reference", "Description", "Amount", "Status");
            r.recentActivity().forEach(x -> add(recent, x.type(), x.date(), x.reference(), x.description(), x.amount(), x.status()));
            document.add(recent);
            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate finance report PDF", ex);
        }
    }

    private static PdfPTable table(String... headers) {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        for (String header : headers) table.addCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        return table;
    }

    private static void add(PdfPTable table, Object... values) {
        for (Object value : values) table.addCell(new Phrase(value == null ? "" : value.toString(),
                FontFactory.getFont(FontFactory.HELVETICA, 8)));
    }
    private static void kpi(StringBuilder out, String name, BigDecimal amount) { out.append(name).append(',').append(amount).append('\n'); }
    private static String cell(String value) { return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\""; }
}
