package com.thinkerscave.finance.payroll.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.payroll.entity.EmployeePayroll;
import com.thinkerscave.finance.payroll.entity.EmployeePayrollLine;
import com.thinkerscave.finance.payroll.entity.PayrollConfiguration;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollLineRepository;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.finance.payroll.service.PayrollSettingsService;
import com.thinkerscave.finance.payroll.service.PayslipService;
import com.thinkerscave.shared.document.dto.StoreDocumentCommand;
import com.thinkerscave.shared.document.entity.ManagedDocument;
import com.thinkerscave.shared.document.enums.DocumentType;
import com.thinkerscave.shared.document.service.DocumentManagementService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayslipServiceImpl implements PayslipService {

    public static final String OWNER_TYPE = "EMPLOYEE_PAYROLL";

    private final EmployeePayrollRepository employeePayrollRepository;
    private final EmployeePayrollLineRepository lineRepository;
    private final StaffRepository staffRepository;
    private final DocumentManagementService documentManagementService;
    private final PayrollSettingsService settingsService;
    private final PayrollAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    @Transactional
    public void freezeAndStore(EmployeePayroll employeePayroll) {
        if (employeePayroll.getPayslipDocumentId() != null) {
            return;
        }
        Staff staff = staffRepository.findById(employeePayroll.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        List<EmployeePayrollLine> lines = lineRepository
                .findByEmployeePayroll_EmployeePayrollIdOrderBySortOrderAsc(employeePayroll.getEmployeePayrollId());
        PayrollConfiguration cfg = settingsService.requireConfig();
        byte[] pdf = buildPdf(employeePayroll, staff, lines, cfg);
        String periodKey = String.format("%04d-%02d", employeePayroll.getPayrollYear(), employeePayroll.getPayrollMonth());
        String idempotencyKey = "PAYSLIP:" + employeePayroll.getEmployeePayrollId();
        String fileName = "payslip-" + employeePayroll.getEmployeePayrollId() + ".pdf";

        ManagedDocument doc = documentManagementService.store(StoreDocumentCommand.builder()
                .documentType(DocumentType.PAYSLIP)
                .ownerType(OWNER_TYPE)
                .ownerId(employeePayroll.getEmployeePayrollId())
                .periodKey(periodKey)
                .idempotencyKey(idempotencyKey)
                .content(pdf)
                .contentType("application/pdf")
                .fileName(fileName)
                .build());

        employeePayroll.setPayslipDocumentId(doc.getManagedDocumentId());
        if (employeePayroll.getPayslipNumber() == null) {
            employeePayroll.setPayslipNumber(formatPayslipNumber(cfg, employeePayroll));
        }
        employeePayrollRepository.save(employeePayroll);
        auditWriteService.record(AuditEventType.CREATE, "PAYSLIP_STORE", "ManagedDocument",
                String.valueOf(doc.getManagedDocumentId()),
                "Stored payslip for employee payroll " + employeePayroll.getEmployeePayrollId());
    }

    @Override
    public byte[] loadStoredPayslip(Long employeePayrollId) {
        EmployeePayroll ep = employeePayrollRepository.findById(employeePayrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee payroll not found: " + employeePayrollId));
        boolean admin = accessGuard.canView(PayrollAccessGuard.RESOURCE_PAYSLIP);
        boolean self = false;
        try {
            if (accessGuard.canView(PayrollAccessGuard.RESOURCE_MY)) {
                accessGuard.assertSelfStaff(ep.getStaffId());
                self = true;
            }
        } catch (Exception ignored) {
            self = false;
        }
        if (!admin && !self) {
            accessGuard.requireView(PayrollAccessGuard.RESOURCE_PAYSLIP);
        }
        if (ep.getPayslipDocumentId() == null) {
            throw new BadRequestException("Payslip not available; payroll has not been frozen");
        }
        return documentManagementService.loadContent(ep.getPayslipDocumentId());
    }

    @Override
    @Transactional
    public void clearPayslipIfUnpaid(EmployeePayroll employeePayroll) {
        employeePayroll.setPayslipDocumentId(null);
        employeePayroll.setPayslipNumber(null);
        employeePayrollRepository.save(employeePayroll);
    }

    private String formatPayslipNumber(PayrollConfiguration cfg, EmployeePayroll ep) {
        String fmt = cfg.getPayslipNumberFormat();
        if (fmt == null || fmt.isBlank()) {
            return String.format("PS-%04d-%02d-%04d", ep.getPayrollYear(), ep.getPayrollMonth(), ep.getEmployeePayrollId());
        }
        return fmt
                .replace("{YYYY}", String.format("%04d", ep.getPayrollYear()))
                .replace("{MM}", String.format("%02d", ep.getPayrollMonth()))
                .replace("{0001}", String.format("%04d", ep.getEmployeePayrollId()));
    }

    private byte[] buildPdf(EmployeePayroll ep, Staff staff, List<EmployeePayrollLine> lines, PayrollConfiguration cfg) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);

            String orgName = cfg.getPayslipOrgName() != null ? cfg.getPayslipOrgName() : "Organization";
            Paragraph heading = new Paragraph(orgName, title);
            heading.setAlignment(Element.ALIGN_CENTER);
            document.add(heading);
            Paragraph slip = new Paragraph("PAYSLIP", label);
            slip.setAlignment(Element.ALIGN_CENTER);
            document.add(slip);

            String monthName = Month.of(ep.getPayrollMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            document.add(new Paragraph("Period: " + monthName + " " + ep.getPayrollYear(), body));
            if (ep.getPayslipNumber() != null) {
                document.add(new Paragraph("Payslip No: " + ep.getPayslipNumber(), body));
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Employee", label));
            document.add(new Paragraph(staff.getFirstName() + " " + staff.getLastName()
                    + " (" + staff.getStaffCode() + ")", body));
            if (ep.getDesignation() != null) {
                document.add(new Paragraph("Designation: " + ep.getDesignation(), body));
            }
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f});
            addHeader(table, "Component", "Amount");

            for (EmployeePayrollLine line : lines) {
                if (line.getComponentType() == ComponentType.EARNING) {
                    addRow(table, line.getComponentName(), line.getAmount(), body);
                }
            }
            addRow(table, "Gross", ep.getGrossAmount(), label);
            for (EmployeePayrollLine line : lines) {
                if (line.getComponentType() == ComponentType.DEDUCTION) {
                    addRow(table, line.getComponentName(), line.getAmount(), body);
                }
            }
            addRow(table, "Total Deductions", ep.getTotalDeductions(), label);
            addRow(table, "Net Pay", ep.getNetAmount(), label);

            boolean hasEmployer = lines.stream().anyMatch(l -> l.getComponentType() == ComponentType.EMPLOYER_CONTRIBUTION);
            if (hasEmployer) {
                addHeader(table, "Employer Contributions (informational)", "Amount");
                for (EmployeePayrollLine line : lines) {
                    if (line.getComponentType() == ComponentType.EMPLOYER_CONTRIBUTION) {
                        addRow(table, line.getComponentName(), line.getAmount(), body);
                    }
                }
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Failed to build payslip PDF: " + ex.getMessage());
        }
    }

    private static void addHeader(PdfPTable table, String left, String right) {
        Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        PdfPCell c1 = new PdfPCell(new Paragraph(left, header));
        PdfPCell c2 = new PdfPCell(new Paragraph(right, header));
        c1.setBackgroundColor(new Color(230, 230, 230));
        c2.setBackgroundColor(new Color(230, 230, 230));
        table.addCell(c1);
        table.addCell(c2);
    }

    private static void addRow(PdfPTable table, String label, BigDecimal amount, Font font) {
        table.addCell(new Paragraph(label, font));
        table.addCell(new Paragraph(amount != null ? amount.toPlainString() : "0.00", font));
    }
}
