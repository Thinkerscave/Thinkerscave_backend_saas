package com.thinkerscave.common.fee.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ConflictException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.fee.domain.*;
import com.thinkerscave.common.fee.dto.FeePaymentAllocationDTO;
import com.thinkerscave.common.fee.dto.FeePaymentDTO;
import com.thinkerscave.common.fee.repository.*;
import com.thinkerscave.common.common.sequence.SequenceGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Records {@link FeePayment} rows and allocates them against
 * {@link FeeInvoice}s via {@link FeePaymentAllocation}. Each allocation
 * triggers a {@link FeeLedgerService#postPayment} ledger credit.
 *
 * <p>Supports partial / advance / over-payments — the residual is left as
 * {@code unallocated_amount} on the payment and can be allocated later.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeePaymentService {

    private final FeePaymentRepository feePaymentRepository;
    private final FeePaymentAllocationRepository feePaymentAllocationRepository;
    private final FeeInvoiceRepository feeInvoiceRepository;
    private final FeeInvoiceService feeInvoiceService;
    private final FeeLedgerService feeLedgerService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final AuditPublisher auditPublisher;

    public Page<FeePaymentDTO> listByStudent(Long studentId, Pageable pageable) {
        return feePaymentRepository
                .findByOrganizationIdAndStudentId(currentOrgId(), studentId, pageable)
                .map(this::toDto);
    }

    public FeePaymentDTO get(Long id) { return toDto(load(id)); }

    @Transactional
    public FeePaymentDTO record(FeePaymentDTO dto) {
        if (dto.getId() != null) {
            throw new BadRequestException("Cannot edit an existing payment; record an adjustment or refund instead");
        }
        Long orgId = currentOrgId();
        FeePayment payment = FeePayment.builder()
                .receiptNumber(dto.getReceiptNumber() != null
                        ? dto.getReceiptNumber()
                        : sequenceGeneratorService.nextNumber(orgId, "RECEIPT", null))
                .studentId(dto.getStudentId())
                .paymentDate(dto.getPaymentDate())
                .amount(dto.getAmount())
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(dto.getAmount())
                .paymentMethod(dto.getPaymentMethod())
                .referenceNumber(dto.getReferenceNumber())
                .gatewayTransactionId(dto.getGatewayTransactionId())
                .receivedByUserId(dto.getReceivedByUserId())
                .status(dto.getStatus() != null ? dto.getStatus() : PaymentStatus.SUCCESS)
                .remarks(dto.getRemarks())
                .build();
        payment.setOrganizationId(orgId);
        payment = feePaymentRepository.save(payment);

        if (dto.getAllocations() != null && !dto.getAllocations().isEmpty()) {
            applyAllocationsInternal(payment, dto.getAllocations());
        }

        auditPublisher.publish(AuditEventType.CREATE, "fee_payment.record",
                "FeePayment", payment.getId(),
                "Recorded payment " + payment.getReceiptNumber()
                        + " amount " + payment.getAmount());
        return toDto(load(payment.getId()));
    }

    /** Apply (additional) allocations to an existing payment that has unallocated balance. */
    @Transactional
    public FeePaymentDTO allocate(Long paymentId, List<FeePaymentAllocationDTO> allocations) {
        FeePayment payment = load(paymentId);
        applyAllocationsInternal(payment, allocations);
        auditPublisher.publish(AuditEventType.UPDATE, "fee_payment.allocate",
                "FeePayment", paymentId, "Applied " + allocations.size() + " allocation(s)");
        return toDto(load(paymentId));
    }

    private void applyAllocationsInternal(FeePayment payment,
                                          List<FeePaymentAllocationDTO> allocations) {
        BigDecimal remaining = nz(payment.getUnallocatedAmount());
        for (FeePaymentAllocationDTO a : allocations) {
            if (a.getAmount() == null || a.getAmount().signum() <= 0) {
                throw new BadRequestException("Allocation amount must be positive");
            }
            if (a.getAmount().compareTo(remaining) > 0) {
                throw new ConflictException("Allocation " + a.getAmount()
                        + " exceeds unallocated amount " + remaining);
            }
            FeeInvoice inv = feeInvoiceRepository.findById(a.getFeeInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + a.getFeeInvoiceId()));
            if (inv.getStatus() == InvoiceStatus.CANCELLED || inv.getStatus() == InvoiceStatus.WRITTEN_OFF) {
                throw new ConflictException("Cannot allocate to invoice in status " + inv.getStatus());
            }

            FeePaymentAllocation alloc = FeePaymentAllocation.builder()
                    .feePaymentId(payment.getId())
                    .feeInvoiceId(inv.getId())
                    .amount(a.getAmount())
                    .remarks(a.getRemarks())
                    .build();
            feePaymentAllocationRepository.save(alloc);

            feeInvoiceService.refreshTotals(inv, a.getAmount());
            feeLedgerService.postPayment(payment, a.getAmount(), inv.getId());

            remaining = remaining.subtract(a.getAmount());
        }
        payment.setUnallocatedAmount(remaining);
        payment.setAllocatedAmount(nz(payment.getAmount()).subtract(remaining));
        feePaymentRepository.save(payment);
    }

    // ---------------------------------------------------------- helpers ---

    FeePayment load(Long id) {
        return feePaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    private Long currentOrgId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) throw new BadRequestException("No organization context");
        return orgId;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    FeePaymentDTO toDto(FeePayment e) {
        List<FeePaymentAllocationDTO> allocs = new ArrayList<>();
        for (FeePaymentAllocation a : feePaymentAllocationRepository.findByFeePaymentId(e.getId())) {
            allocs.add(FeePaymentAllocationDTO.builder()
                    .id(a.getId())
                    .feeInvoiceId(a.getFeeInvoiceId())
                    .amount(a.getAmount())
                    .remarks(a.getRemarks())
                    .build());
        }
        return FeePaymentDTO.builder()
                .id(e.getId())
                .receiptNumber(e.getReceiptNumber())
                .studentId(e.getStudentId())
                .paymentDate(e.getPaymentDate())
                .amount(e.getAmount())
                .allocatedAmount(e.getAllocatedAmount())
                .unallocatedAmount(e.getUnallocatedAmount())
                .paymentMethod(e.getPaymentMethod())
                .referenceNumber(e.getReferenceNumber())
                .gatewayTransactionId(e.getGatewayTransactionId())
                .receivedByUserId(e.getReceivedByUserId())
                .status(e.getStatus())
                .remarks(e.getRemarks())
                .allocations(allocs)
                .build();
    }
}
