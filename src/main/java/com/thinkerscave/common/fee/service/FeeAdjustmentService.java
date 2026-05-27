package com.thinkerscave.common.fee.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ConflictException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.fee.domain.*;
import com.thinkerscave.common.fee.dto.FeeAdjustmentDTO;
import com.thinkerscave.common.fee.dto.FeeRefundDTO;
import com.thinkerscave.common.fee.dto.FeeReminderDTO;
import com.thinkerscave.common.fee.repository.*;
import com.thinkerscave.common.common.sequence.SequenceGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Handles secondary fee-side workflows that do not warrant their own service
 * class: adjustments, refunds, and reminders. Each write posts the appropriate
 * ledger entry via {@link FeeLedgerService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeeAdjustmentService {

    private final FeeAdjustmentRepository feeAdjustmentRepository;
    private final FeeRefundRepository feeRefundRepository;
    private final FeeReminderRepository feeReminderRepository;
    private final FeeInvoiceRepository feeInvoiceRepository;
    private final FeeLedgerService feeLedgerService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final AuditPublisher auditPublisher;

    // ----------------------------------------------------- Adjustments ----

    public Page<FeeAdjustmentDTO> listAdjustments(Long studentId, Pageable pageable) {
        return feeAdjustmentRepository
                .findByOrganizationIdAndStudentId(currentOrgId(), studentId, pageable)
                .map(this::toDto);
    }

    @Transactional
    public FeeAdjustmentDTO createAdjustment(FeeAdjustmentDTO dto) {
        Long orgId = currentOrgId();
        if (dto.getAmount() == null || dto.getAmount().signum() <= 0) {
            throw new BadRequestException("Adjustment amount must be positive");
        }
        FeeAdjustment adj = FeeAdjustment.builder()
                .feeInvoiceId(dto.getFeeInvoiceId())
                .studentId(dto.getStudentId())
                .adjustmentType(dto.getAdjustmentType())
                .amount(dto.getAmount())
                .effectiveDate(dto.getEffectiveDate() != null ? dto.getEffectiveDate() : LocalDate.now())
                .approvedByUserId(dto.getApprovedByUserId())
                .reason(dto.getReason())
                .build();
        adj.setOrganizationId(orgId);
        FeeAdjustment saved = feeAdjustmentRepository.save(adj);
        feeLedgerService.postAdjustment(saved);

        auditPublisher.publish(AuditEventType.CREATE, "fee_adjustment.create",
                "FeeAdjustment", saved.getId(),
                "Adjustment " + saved.getAdjustmentType() + " " + saved.getAmount());
        return toDto(saved);
    }

    // ---------------------------------------------------------- Refunds ---

    public Page<FeeRefundDTO> listRefunds(RefundStatus status, Pageable pageable) {
        return feeRefundRepository
                .findByOrganizationIdAndStatus(currentOrgId(), status, pageable)
                .map(this::toDto);
    }

    @Transactional
    public FeeRefundDTO requestRefund(FeeRefundDTO dto) {
        Long orgId = currentOrgId();
        FeeRefund refund = FeeRefund.builder()
                .refundNumber(sequenceGeneratorService.nextNumber(orgId, "REFUND", null))
                .studentId(dto.getStudentId())
                .feePaymentId(dto.getFeePaymentId())
                .feeInvoiceId(dto.getFeeInvoiceId())
                .amount(dto.getAmount())
                .reason(dto.getReason())
                .status(RefundStatus.REQUESTED)
                .requestedOn(LocalDate.now())
                .payoutMethod(dto.getPayoutMethod())
                .build();
        refund.setOrganizationId(orgId);
        FeeRefund saved = feeRefundRepository.save(refund);
        auditPublisher.publish(AuditEventType.CREATE, "fee_refund.request",
                "FeeRefund", saved.getId(),
                "Requested refund " + saved.getRefundNumber() + " amount " + saved.getAmount());
        return toDto(saved);
    }

    @Transactional
    public FeeRefundDTO approveRefund(Long id, Long approvedByUserId) {
        FeeRefund refund = loadRefund(id);
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new ConflictException("Cannot approve refund in status " + refund.getStatus());
        }
        refund.setStatus(RefundStatus.APPROVED);
        refund.setApprovedOn(LocalDate.now());
        refund.setApprovedByUserId(approvedByUserId);
        FeeRefund saved = feeRefundRepository.save(refund);
        auditPublisher.publish(AuditEventType.APPROVAL, "fee_refund.approve",
                "FeeRefund", id, "Approved refund " + refund.getRefundNumber());
        return toDto(saved);
    }

    @Transactional
    public FeeRefundDTO processRefund(Long id, String payoutReference) {
        FeeRefund refund = loadRefund(id);
        if (refund.getStatus() != RefundStatus.APPROVED) {
            throw new ConflictException("Cannot process refund in status " + refund.getStatus());
        }
        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProcessedOn(LocalDate.now());
        refund.setPayoutReference(payoutReference);
        FeeRefund saved = feeRefundRepository.save(refund);
        feeLedgerService.postRefund(saved);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "fee_refund.process",
                "FeeRefund", id, "Processed refund " + refund.getRefundNumber());
        return toDto(saved);
    }

    @Transactional
    public FeeRefundDTO rejectRefund(Long id, String reason) {
        FeeRefund refund = loadRefund(id);
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new ConflictException("Cannot reject refund in status " + refund.getStatus());
        }
        refund.setStatus(RefundStatus.REJECTED);
        refund.setReason(refund.getReason() + " | REJECTED: " + reason);
        FeeRefund saved = feeRefundRepository.save(refund);
        auditPublisher.publish(AuditEventType.REJECTION, "fee_refund.reject",
                "FeeRefund", id, "Rejected refund " + refund.getRefundNumber());
        return toDto(saved);
    }

    // -------------------------------------------------------- Reminders ---

    public List<FeeReminderDTO> listReminders(Long invoiceId) {
        return feeReminderRepository.findByFeeInvoiceId(invoiceId).stream()
                .map(this::toDto).toList();
    }

    /** Record that a reminder was dispatched (caller is responsible for the actual delivery). */
    @Transactional
    public FeeReminderDTO recordReminderDispatch(Long invoiceId,
                                                 ReminderChannel channel,
                                                 String recipient,
                                                 String subject,
                                                 boolean success,
                                                 String failureReason) {
        FeeInvoice inv = feeInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        long previousAttempts = feeReminderRepository.countByFeeInvoiceId(invoiceId);
        FeeReminder reminder = FeeReminder.builder()
                .feeInvoiceId(invoiceId)
                .studentId(inv.getStudentId())
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .sentAt(Instant.now())
                .success(success)
                .failureReason(failureReason)
                .attemptNumber((int) previousAttempts + 1)
                .build();
        return toDto(feeReminderRepository.save(reminder));
    }

    // ---------------------------------------------------------- helpers ---

    private FeeRefund loadRefund(Long id) {
        return feeRefundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + id));
    }

    private Long currentOrgId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) throw new BadRequestException("No organization context");
        return orgId;
    }

    private FeeAdjustmentDTO toDto(FeeAdjustment e) {
        return FeeAdjustmentDTO.builder()
                .id(e.getId())
                .feeInvoiceId(e.getFeeInvoiceId())
                .studentId(e.getStudentId())
                .adjustmentType(e.getAdjustmentType())
                .amount(e.getAmount())
                .effectiveDate(e.getEffectiveDate())
                .approvedByUserId(e.getApprovedByUserId())
                .reason(e.getReason())
                .build();
    }

    private FeeRefundDTO toDto(FeeRefund e) {
        return FeeRefundDTO.builder()
                .id(e.getId())
                .refundNumber(e.getRefundNumber())
                .studentId(e.getStudentId())
                .feePaymentId(e.getFeePaymentId())
                .feeInvoiceId(e.getFeeInvoiceId())
                .amount(e.getAmount())
                .reason(e.getReason())
                .status(e.getStatus())
                .requestedOn(e.getRequestedOn())
                .approvedOn(e.getApprovedOn())
                .processedOn(e.getProcessedOn())
                .approvedByUserId(e.getApprovedByUserId())
                .payoutMethod(e.getPayoutMethod())
                .payoutReference(e.getPayoutReference())
                .build();
    }

    private FeeReminderDTO toDto(FeeReminder e) {
        return FeeReminderDTO.builder()
                .id(e.getId())
                .feeInvoiceId(e.getFeeInvoiceId())
                .studentId(e.getStudentId())
                .channel(e.getChannel())
                .recipient(e.getRecipient())
                .subject(e.getSubject())
                .sentAt(e.getSentAt())
                .success(e.isSuccess())
                .failureReason(e.getFailureReason())
                .attemptNumber(e.getAttemptNumber())
                .build();
    }
}
