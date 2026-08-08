package com.thinkerscave.staff.service;

import com.thinkerscave.staff.entity.Payroll;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.entity.StaffSalaryStructure;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Builds a simple Phase-1 payslip PDF from a payroll snapshot.
 */
@Service
public class PayslipPdfService {

    public byte[] buildPayslip(Payroll payroll, StaffSalaryStructure structure) {
        Staff staff = payroll.getStaff();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph heading = new Paragraph("PAYSLIP", title);
            heading.setAlignment(Element.ALIGN_CENTER);
            document.add(heading);

            String monthName = Month.of(payroll.getPayrollMonth())
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            document.add(new Paragraph("Period: " + monthName + " " + payroll.getPayrollYear(), body));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Employee", label));
            document.add(new Paragraph(staff.getFirstName() + " " + staff.getLastName()
                    + " (" + staff.getStaffCode() + ")", body));
            if (staff.getDesignation() != null) {
                document.add(new Paragraph("Designation: " + staff.getDesignation(), body));
            }
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f});
            addHeader(table, "Earnings / Deductions", "Amount (₹)");

            if (structure != null) {
                addRow(table, "Basic Pay", structure.getBasicPay(), body);
                addRow(table, "HRA", structure.getHra(), body);
                addRow(table, "DA", structure.getDa(), body);
                addRow(table, "Special Allowance", structure.getSpecialAllowance(), body);
                addRow(table, "Transport Allowance", structure.getTransportAllowance(), body);
                addRow(table, "Other Allowance", structure.getOtherAllowance(), body);
            }
            addRow(table, "Gross Salary", payroll.getGrossSalary(), label);

            addRow(table, "PF (Employee)", payroll.getPfAmount(), body);
            addRow(table, "ESI (Employee)", payroll.getEsiAmount(), body);
            addRow(table, "Professional Tax", payroll.getProfessionalTaxAmount(), body);
            addRow(table, "Other Deduction", payroll.getOtherDeductionAmount(), body);
            if (payroll.getLeaveWithoutPayDays() != null && payroll.getLeaveWithoutPayDays() > 0) {
                addRow(table, "LOP Days", BigDecimal.valueOf(payroll.getLeaveWithoutPayDays()), body);
            }
            addRow(table, "Total Deductions", payroll.getTotalDeductions(), label);
            addRow(table, "Net Salary", payroll.getNetSalary(), label);

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Status: " + payroll.getStatus(), body));
            if (payroll.getPaidOn() != null) {
                document.add(new Paragraph("Paid On: " + payroll.getPaidOn(), body));
            }
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "This is a system-generated payslip. Leave Without Pay (LOP) is not applied until Leave Management is available.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8)));

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate payslip PDF", ex);
        }
    }

    private static void addHeader(PdfPTable table, String left, String right) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell c1 = new PdfPCell(new Phrase(left, headerFont));
        PdfPCell c2 = new PdfPCell(new Phrase(right, headerFont));
        c1.setBackgroundColor(new Color(31, 58, 147));
        c2.setBackgroundColor(new Color(31, 58, 147));
        c1.setPadding(6);
        c2.setPadding(6);
        table.addCell(c1);
        table.addCell(c2);
    }

    private static void addRow(PdfPTable table, String name, BigDecimal amount, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(name, font));
        PdfPCell c2 = new PdfPCell(new Phrase(formatMoney(amount), font));
        c1.setPadding(5);
        c2.setPadding(5);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);
        table.addCell(c2);
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
