package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.response.FeeReceiptLineResponse;
import com.thinkerscave.finance.dto.response.FeeReceiptResponse;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * OpenPDF receipt renderer from immutable fee_receipt + fee_receipt_line snapshots.
 */
@Service
public class FeeReceiptPdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public byte[] build(FeeReceiptResponse receipt) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 9);

            Paragraph heading = new Paragraph("FEE RECEIPT", title);
            heading.setAlignment(Element.ALIGN_CENTER);
            document.add(heading);
            document.add(new Paragraph(" "));

            if (receipt.getSchoolName() != null) {
                document.add(new Paragraph(receipt.getSchoolName(), label));
            }
            if (receipt.getSchoolAddress() != null) {
                document.add(new Paragraph(receipt.getSchoolAddress(), small));
            }
            if (receipt.getSchoolContact() != null) {
                document.add(new Paragraph(receipt.getSchoolContact(), small));
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Receipt No: " + nullSafe(receipt.getReceiptNumber()), body));
            if (receipt.getIssuedOn() != null) {
                document.add(new Paragraph("Issued On: " + DATE_TIME.format(receipt.getIssuedOn()), body));
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Student", label));
            document.add(new Paragraph(nullSafe(receipt.getStudentName())
                    + " (" + nullSafe(receipt.getAdmissionNumber()) + ")", body));
            String classLine = nullSafe(receipt.getClassName());
            if (receipt.getSectionName() != null && !receipt.getSectionName().isBlank()) {
                classLine = classLine + " / " + receipt.getSectionName();
            }
            document.add(new Paragraph("Class: " + classLine, body));
            document.add(new Paragraph("Academic Year: " + nullSafe(receipt.getAcademicYearName()), body));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.5f, 1.5f});
            addHeader(table, "Period covered", "Amount");

            List<FeeReceiptLineResponse> lines = receipt.getLines() == null ? List.of() : receipt.getLines();
            Font rowFont = body;
            for (FeeReceiptLineResponse line : lines) {
                addRow(table, nullSafe(line.getPeriodLabel()), line.getAmount(), rowFont);
            }
            if (lines.isEmpty()) {
                addRow(table, "Payment", receipt.getAmount(), rowFont);
            }
            addRow(table, "Total Paid", receipt.getAmount(), label);
            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Payment Method: " + nullSafe(receipt.getPaymentMethodName()), body));
            if (receipt.getReferenceNumber() != null && !receipt.getReferenceNumber().isBlank()) {
                document.add(new Paragraph("Reference: " + receipt.getReferenceNumber(), body));
            }
            if (receipt.getRemarks() != null && !receipt.getRemarks().isBlank()) {
                document.add(new Paragraph("Remarks: " + receipt.getRemarks(), body));
            }
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "This is a system-generated receipt. Historical amounts remain unchanged if fee configuration changes later.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8)));

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate fee receipt PDF", ex);
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

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
