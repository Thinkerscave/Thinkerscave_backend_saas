package com.thinkerscave.platform.service;

import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.entity.OrganizationSubscription;
import com.thinkerscave.platform.entity.SubscriptionPlan;
import com.thinkerscave.platform.enums.SubscriptionStatus;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Renders the onboarding invoice from existing organization + subscription fields.
 * Invoice number matches {@link com.thinkerscave.platform.service.impl.OrganizationServiceImpl}.
 */
@Service
public class OrganizationInvoicePdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static String invoiceNumber(String organizationCode, Long subscriptionId) {
        if (organizationCode == null || subscriptionId == null) {
            return null;
        }
        return "INV-" + organizationCode + "-" + String.format("%06d", subscriptionId);
    }

    public byte[] build(Organization org, OrganizationSubscription subscription) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph heading = new Paragraph("ONBOARDING INVOICE", title);
            heading.setAlignment(Element.ALIGN_CENTER);
            document.add(heading);
            document.add(new Paragraph(" "));

            String invoiceNo = invoiceNumber(org.getOrganizationCode(), subscription.getId());
            document.add(new Paragraph("Invoice No. " + nvl(invoiceNo), label));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Billed to", label));
            document.add(new Paragraph(nvl(org.getOrganizationName())
                    + " (" + nvl(org.getOrganizationCode()) + ")", body));
            Customer customer = org.getCustomer();
            if (customer != null) {
                document.add(new Paragraph(
                        "Customer: " + nvl(customer.getCustomerName())
                                + " (" + nvl(customer.getCustomerCode()) + ")", body));
            }
            document.add(new Paragraph(" "));

            SubscriptionPlan plan = subscription.getSubscriptionPlan();
            SubscriptionStatus status = subscription.getStatus();
            boolean trial = status == SubscriptionStatus.TRIAL;
            String currency = org.getCurrency() != null ? org.getCurrency() : "INR";
            BigDecimal amountPaid = trial ? BigDecimal.ZERO : subscription.getFinalAmount();

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f});
            addHeader(table, "Item", "Value");
            addTextRow(table, "Invoice Type", trial ? "Free trial" : "Paid", body);
            addTextRow(table, "Payment Status", paymentStatus(status, trial), body);
            addTextRow(table, "Plan", plan != null ? nvl(plan.getPlanName()) : "—", body);
            addTextRow(table, "Plan Code", plan != null ? nvl(plan.getPlanCode()) : "—", body);
            addTextRow(table, "Subscription Status", status != null ? status.name() : "—", body);
            addTextRow(table, "Billing Cycle",
                    subscription.getBillingCycle() != null ? subscription.getBillingCycle().name() : "—", body);
            addTextRow(table, "Start Date", formatDate(subscription.getStartDate()), body);
            addTextRow(table, "Expiry Date", formatDate(subscription.getEndDate()), body);
            addTextRow(table, "Trial Ends", formatDate(subscription.getTrialEndDate()), body);
            addTextRow(table, "Auto Renew", Boolean.TRUE.equals(subscription.getAutoRenew()) ? "On" : "Off", body);
            addTextRow(table, "Plan Price", money(subscription.getPlanPrice(), currency), body);
            addTextRow(table, "Discount", money(subscription.getDiscountAmount(), currency), body);
            addTextRow(table, "Final Amount", money(subscription.getFinalAmount(), currency), body);
            addTextRow(table, "Amount Paid", money(amountPaid, currency), body);
            if (subscription.getPromotion() != null) {
                addTextRow(table, "Coupon", nvl(subscription.getPromotion().getPromotionCode()), body);
            }
            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "This is a system-generated invoice from the organization subscription captured at onboarding.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8)));

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate onboarding invoice PDF", ex);
        }
    }

    private static String paymentStatus(SubscriptionStatus status, boolean trial) {
        if (trial) {
            return "Unpaid";
        }
        if (status == SubscriptionStatus.ACTIVE) {
            return "Paid";
        }
        if (status == SubscriptionStatus.EXPIRED) {
            return "Overdue";
        }
        return status != null ? status.name() : "—";
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

    private static void addTextRow(PdfPTable table, String name, String value, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(name, font));
        PdfPCell c2 = new PdfPCell(new Phrase(value == null || value.isBlank() ? "—" : value, font));
        c1.setPadding(5);
        c2.setPadding(5);
        table.addCell(c1);
        table.addCell(c2);
    }

    private static String money(BigDecimal amount, String currency) {
        String value = amount == null
                ? "0.00"
                : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        if ("INR".equalsIgnoreCase(currency)) {
            return "₹" + value;
        }
        return (currency == null ? "" : currency + " ") + value;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    private static String nvl(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
