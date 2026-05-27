package com.thinkerscave.common.fee.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ConflictException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.fee.domain.*;
import com.thinkerscave.common.fee.dto.FeeInvoiceDTO;
import com.thinkerscave.common.fee.dto.FeeInvoiceLineDTO;
import com.thinkerscave.common.fee.repository.*;
import com.thinkerscave.common.common.sequence.SequenceGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fee-invoice queries + invoice generation from a {@link FeeContract}.
 *
 * <p>Generation strategy (scaffold version):
 * <ul>
 *     <li>For each periodic {@link FeeStructureItem} due in the requested
 *         period, emit one {@link FeeInvoiceLine}.</li>
 *     <li>{@link FeeInvoice} totals are derived from the lines.</li>
 *     <li>Invoice number comes from the {@code INV} sequence.</li>
 *     <li>Ledger posting is delegated to {@link FeeLedgerService}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeeInvoiceService {

    private final FeeInvoiceRepository feeInvoiceRepository;
    private final FeeInvoiceLineRepository feeInvoiceLineRepository;
    private final FeeContractRepository feeContractRepository;
    private final FeeStructureItemRepository feeStructureItemRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final FeeLedgerService feeLedgerService;
    private final AuditPublisher auditPublisher;

    public Page<FeeInvoiceDTO> listByStudent(Long studentId, Pageable pageable) {
        return feeInvoiceRepository
                .findByOrganizationIdAndStudentId(currentOrgId(), studentId, pageable)
                .map(this::toDto);
    }

    public Page<FeeInvoiceDTO> listByStatus(InvoiceStatus status, Pageable pageable) {
        return feeInvoiceRepository
                .findByOrganizationIdAndStatus(currentOrgId(), status, pageable)
                .map(this::toDto);
    }

    public FeeInvoiceDTO get(Long id) { return toDto(load(id)); }

    public FeeInvoiceDTO getByNumber(String invoiceNumber) {
        return feeInvoiceRepository.findByOrganizationIdAndInvoiceNumber(currentOrgId(), invoiceNumber)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceNumber));
    }

    /**
     * Generate one invoice for the given contract for the given billing period
     * (e.g. {@code "2026-MAY"}). Idempotency: refuses to generate if an
     * invoice for the same contract + period already exists.
     */
    @Transactional
    public FeeInvoiceDTO generateForContract(Long contractId,
                                             String periodLabel,
                                             LocalDate issueDate,
                                             LocalDate dueDate) {
        Long orgId = currentOrgId();
        FeeContract contract = feeContractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee contract not found: " + contractId));

        boolean duplicate = feeInvoiceRepository.findAll((root, q, cb) -> cb.and(
                cb.equal(root.get("feeContractId"), contractId),
                cb.equal(root.get("periodLabel"), periodLabel),
                cb.equal(root.get("organizationId"), orgId)
        )).stream().findAny().isPresent();
        if (duplicate) {
            throw new ConflictException("Invoice already generated for contract " + contractId + " period " + periodLabel);
        }

        List<FeeStructureItem> items = feeStructureItemRepository
                .findByFeeStructureId(contract.getFeeStructureId());
        if (items.isEmpty()) {
            throw new BadRequestException("Fee structure has no items: cannot generate invoice");
        }

        FeeInvoice invoice = FeeInvoice.builder()
                .invoiceNumber(sequenceGeneratorService.nextNumber(orgId, "INVOICE", null))
                .feeContractId(contract.getId())
                .studentId(contract.getStudentId())
                .enrollmentId(contract.getEnrollmentId())
                .academicYearId(contract.getAcademicYearId())
                .periodLabel(periodLabel)
                .issueDate(issueDate != null ? issueDate : LocalDate.now())
                .dueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(15))
                .subtotal(BigDecimal.ZERO)
                .discountTotal(BigDecimal.ZERO)
                .taxTotal(BigDecimal.ZERO)
                .lateFeeTotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .balanceAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.ISSUED)
                .build();
        invoice.setOrganizationId(orgId);
        invoice = feeInvoiceRepository.save(invoice);

        BigDecimal subtotal = BigDecimal.ZERO;
        int order = 0;
        for (FeeStructureItem it : items) {
            if (it.isOptional()) continue;
            FeeInvoiceLine line = FeeInvoiceLine.builder()
                    .feeInvoiceId(invoice.getId())
                    .feeHeadId(it.getFeeHeadId())
                    .description(null)
                    .amount(it.getAmount())
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .lineTotal(it.getAmount())
                    .displayOrder(order++)
                    .build();
            feeInvoiceLineRepository.save(line);
            subtotal = subtotal.add(it.getAmount());
        }

        invoice.setSubtotal(subtotal);
        invoice.setTotalAmount(subtotal);
        invoice.setBalanceAmount(subtotal);
        invoice = feeInvoiceRepository.save(invoice);

        feeLedgerService.postInvoice(invoice);

        auditPublisher.publish(AuditEventType.CREATE, "fee_invoice.generate",
                "FeeInvoice", invoice.getId(),
                "Generated invoice " + invoice.getInvoiceNumber() + " period " + periodLabel);
        return toDto(invoice);
    }

    /** Cancel an invoice (soft state change) and post a reversing ledger entry. */
    @Transactional
    public FeeInvoiceDTO cancel(Long id, String reason) {
        FeeInvoice inv = load(id);
        if (inv.getStatus() == InvoiceStatus.PAID || inv.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ConflictException("Invoice in status " + inv.getStatus() + " cannot be cancelled");
        }
        inv.setStatus(InvoiceStatus.CANCELLED);
        inv.setNotes(reason);
        FeeInvoice saved = feeInvoiceRepository.save(inv);
        feeLedgerService.postInvoiceReversal(saved);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "fee_invoice.cancel",
                "FeeInvoice", id, "Cancelled invoice " + saved.getInvoiceNumber());
        return toDto(saved);
    }

    /** Recompute paid + balance + status from existing allocations. */
    @Transactional
    public void refreshTotals(FeeInvoice invoice, BigDecimal additionalPaid) {
        BigDecimal newPaid = nz(invoice.getPaidAmount()).add(nz(additionalPaid));
        BigDecimal balance = nz(invoice.getTotalAmount()).subtract(newPaid);
        invoice.setPaidAmount(newPaid);
        invoice.setBalanceAmount(balance.max(BigDecimal.ZERO));
        if (balance.signum() <= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (newPaid.signum() > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        feeInvoiceRepository.save(invoice);
    }

    FeeInvoice load(Long id) {
        return feeInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    private Long currentOrgId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) throw new BadRequestException("No organization context");
        return orgId;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    FeeInvoiceDTO toDto(FeeInvoice e) {
        List<FeeInvoiceLineDTO> lines = new ArrayList<>();
        for (FeeInvoiceLine line : feeInvoiceLineRepository.findByFeeInvoiceId(e.getId())) {
            lines.add(FeeInvoiceLineDTO.builder()
                    .id(line.getId())
                    .feeHeadId(line.getFeeHeadId())
                    .description(line.getDescription())
                    .amount(line.getAmount())
                    .discountAmount(line.getDiscountAmount())
                    .taxAmount(line.getTaxAmount())
                    .lineTotal(line.getLineTotal())
                    .displayOrder(line.getDisplayOrder())
                    .build());
        }
        return FeeInvoiceDTO.builder()
                .id(e.getId())
                .invoiceNumber(e.getInvoiceNumber())
                .feeContractId(e.getFeeContractId())
                .studentId(e.getStudentId())
                .enrollmentId(e.getEnrollmentId())
                .academicYearId(e.getAcademicYearId())
                .periodLabel(e.getPeriodLabel())
                .issueDate(e.getIssueDate())
                .dueDate(e.getDueDate())
                .subtotal(e.getSubtotal())
                .discountTotal(e.getDiscountTotal())
                .taxTotal(e.getTaxTotal())
                .lateFeeTotal(e.getLateFeeTotal())
                .totalAmount(e.getTotalAmount())
                .paidAmount(e.getPaidAmount())
                .balanceAmount(e.getBalanceAmount())
                .status(e.getStatus())
                .notes(e.getNotes())
                .lines(lines)
                .build();
    }
}
