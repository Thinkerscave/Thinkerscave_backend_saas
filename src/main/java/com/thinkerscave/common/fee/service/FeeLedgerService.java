package com.thinkerscave.common.fee.service;

import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.fee.domain.*;
import com.thinkerscave.common.fee.dto.FeeLedgerEntryDTO;
import com.thinkerscave.common.fee.repository.FeeLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Append-only ledger writer + reader. All ledger writes go through this
 * service so the {@code running_balance} is always coherent for a student.
 *
 * <p>Conventions:
 * <ul>
 *     <li>Invoice issued      → debit  (student owes more)</li>
 *     <li>Payment received    → credit (student owes less)</li>
 *     <li>Adjustment discount → credit</li>
 *     <li>Adjustment penalty  → debit</li>
 *     <li>Refund processed    → debit  (org pays back)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FeeLedgerService {

    private final FeeLedgerEntryRepository feeLedgerEntryRepository;

    public void postInvoice(FeeInvoice invoice) {
        appendDebit(invoice.getStudentId(), invoice.getTotalAmount(),
                "INVOICE", invoice.getId(), null, null, null,
                "Invoice " + invoice.getInvoiceNumber());
    }

    public void postInvoiceReversal(FeeInvoice invoice) {
        appendCredit(invoice.getStudentId(), invoice.getTotalAmount(),
                "INVOICE_REVERSAL", invoice.getId(), null, null, null,
                "Cancelled invoice " + invoice.getInvoiceNumber());
    }

    public void postPayment(FeePayment payment, BigDecimal allocatedAmount, Long invoiceId) {
        appendCredit(payment.getStudentId(), allocatedAmount,
                "PAYMENT", invoiceId, payment.getId(), null, null,
                "Payment receipt " + payment.getReceiptNumber());
    }

    public void postAdjustment(FeeAdjustment adj) {
        boolean credit = switch (adj.getAdjustmentType()) {
            case PENALTY, LATE_FEE -> false;
            default -> true;
        };
        String desc = adj.getAdjustmentType() + " adjustment";
        if (credit) {
            appendCredit(adj.getStudentId(), adj.getAmount(),
                    "ADJUSTMENT", adj.getFeeInvoiceId(), null, adj.getId(), null, desc);
        } else {
            appendDebit(adj.getStudentId(), adj.getAmount(),
                    "ADJUSTMENT", adj.getFeeInvoiceId(), null, adj.getId(), null, desc);
        }
    }

    public void postRefund(FeeRefund refund) {
        appendDebit(refund.getStudentId(), refund.getAmount(),
                "REFUND", refund.getFeeInvoiceId(), refund.getFeePaymentId(), null, refund.getId(),
                "Refund " + refund.getRefundNumber());
    }

    @Transactional(readOnly = true)
    public Page<FeeLedgerEntryDTO> statementOf(Long studentId, Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        return feeLedgerEntryRepository
                .findByOrganizationIdAndStudentIdOrderByEntryDateAscIdAsc(orgId, studentId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public BigDecimal currentBalance(Long studentId) {
        Long orgId = OrganizationContext.getOrganizationId();
        // last entry's running balance — cheap & coherent because all writes update it.
        return feeLedgerEntryRepository
                .findByOrganizationIdAndStudentIdOrderByEntryDateAscIdAsc(orgId, studentId,
                        Pageable.unpaged())
                .stream()
                .reduce((a, b) -> b)
                .map(FeeLedgerEntry::getRunningBalance)
                .orElse(BigDecimal.ZERO);
    }

    // ---------------------------------------------------------- internal --

    private void appendDebit(Long studentId, BigDecimal amount, String type,
                             Long invoiceId, Long paymentId, Long adjId, Long refundId,
                             String description) {
        appendInternal(studentId, amount, BigDecimal.ZERO, type,
                invoiceId, paymentId, adjId, refundId, description);
    }

    private void appendCredit(Long studentId, BigDecimal amount, String type,
                              Long invoiceId, Long paymentId, Long adjId, Long refundId,
                              String description) {
        appendInternal(studentId, BigDecimal.ZERO, amount, type,
                invoiceId, paymentId, adjId, refundId, description);
    }

    private void appendInternal(Long studentId, BigDecimal debit, BigDecimal credit,
                                String type, Long invoiceId, Long paymentId, Long adjId,
                                Long refundId, String description) {
        Long orgId = OrganizationContext.getOrganizationId();
        BigDecimal newBalance = currentBalanceInternal(orgId, studentId)
                .add(nz(debit)).subtract(nz(credit));

        FeeLedgerEntry entry = FeeLedgerEntry.builder()
                .organizationId(orgId)
                .studentId(studentId)
                .entryDate(LocalDate.now())
                .entryType(type)
                .debitAmount(nz(debit))
                .creditAmount(nz(credit))
                .runningBalance(newBalance)
                .feeInvoiceId(invoiceId)
                .feePaymentId(paymentId)
                .feeAdjustmentId(adjId)
                .feeRefundId(refundId)
                .description(description)
                .build();
        feeLedgerEntryRepository.save(entry);
    }

    private BigDecimal currentBalanceInternal(Long orgId, Long studentId) {
        List<FeeLedgerEntry> entries = feeLedgerEntryRepository
                .findByOrganizationIdAndStudentIdOrderByEntryDateAscIdAsc(orgId, studentId,
                        Pageable.unpaged())
                .getContent();
        if (entries.isEmpty()) return BigDecimal.ZERO;
        return entries.get(entries.size() - 1).getRunningBalance();
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private FeeLedgerEntryDTO toDto(FeeLedgerEntry e) {
        return FeeLedgerEntryDTO.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .entryDate(e.getEntryDate())
                .entryType(e.getEntryType())
                .debitAmount(e.getDebitAmount())
                .creditAmount(e.getCreditAmount())
                .runningBalance(e.getRunningBalance())
                .feeInvoiceId(e.getFeeInvoiceId())
                .feePaymentId(e.getFeePaymentId())
                .feeAdjustmentId(e.getFeeAdjustmentId())
                .feeRefundId(e.getFeeRefundId())
                .description(e.getDescription())
                .build();
    }
}
