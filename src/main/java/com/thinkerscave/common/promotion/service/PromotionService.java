package com.thinkerscave.common.promotion.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.enrollment.domain.AcademicEnrollment;
import com.thinkerscave.common.enrollment.domain.EnrollmentStatus;
import com.thinkerscave.common.enrollment.repository.AcademicEnrollmentRepository;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.promotion.domain.PromotionBatch;
import com.thinkerscave.common.promotion.domain.PromotionDecision;
import com.thinkerscave.common.promotion.domain.PromotionRecord;
import com.thinkerscave.common.promotion.domain.PromotionStatus;
import com.thinkerscave.common.promotion.dto.PromotionBatchDTO;
import com.thinkerscave.common.promotion.dto.PromotionRecordDTO;
import com.thinkerscave.common.promotion.repository.PromotionBatchRepository;
import com.thinkerscave.common.promotion.repository.PromotionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Year-end mass promotion workflow — create a batch, preview candidate students
 * from the source year/class, persist per-student decisions, and execute or
 * roll back the batch.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PromotionService {

    private final PromotionBatchRepository batchRepository;
    private final PromotionRecordRepository recordRepository;
    private final AcademicEnrollmentRepository enrollmentRepository;
    private final AuditPublisher auditPublisher;

    public Page<PromotionBatchDTO> list(Pageable pageable) {
        return batchRepository.findByOrganizationId(currentOrgId(), pageable).map(this::toDto);
    }

    public PromotionBatchDTO get(Long id) {
        return toDto(load(id));
    }

    public List<PromotionRecordDTO> records(Long batchId) {
        load(batchId);
        return recordRepository.findByPromotionBatchId(batchId).stream().map(this::toDto).toList();
    }

    @Transactional
    public PromotionBatchDTO createBatch(PromotionBatchDTO dto) {
        if (dto.getBatchCode() == null || dto.getBatchCode().isBlank()) {
            throw new BadRequestException("batchCode is required");
        }
        if (dto.getFromAcademicYearId() == null || dto.getToAcademicYearId() == null) {
            throw new BadRequestException("fromAcademicYearId and toAcademicYearId are required");
        }
        Long orgId = currentOrgId();
        PromotionBatch b = PromotionBatch.builder()
                .batchCode(dto.getBatchCode())
                .fromAcademicYearId(dto.getFromAcademicYearId())
                .toAcademicYearId(dto.getToAcademicYearId())
                .fromClassId(dto.getFromClassId())
                .toClassId(dto.getToClassId())
                .status(PromotionStatus.DRAFT)
                .plannedCount(0)
                .processedCount(0)
                .remarks(dto.getRemarks())
                .build();
        b.setOrganizationId(orgId);
        b = batchRepository.save(b);
        auditPublisher.publish(AuditEventType.CREATE, "PROMOTION_BATCH_CREATE", "PromotionBatch",
                b.getId(), "Promotion batch " + b.getBatchCode() + " created");
        return toDto(b);
    }

    /**
     * Build candidate {@link PromotionRecord} rows for every ACTIVE enrollment
     * in the source year/class. Defaults each decision to PROMOTED. Caller may
     * override individual decisions via {@link #updateRecord(Long, PromotionRecordDTO)}.
     */
    @Transactional
    public List<PromotionRecordDTO> preview(Long batchId) {
        PromotionBatch b = load(batchId);
        if (b.getStatus() != PromotionStatus.DRAFT) {
            throw new BadRequestException("Preview is only allowed for DRAFT batches");
        }
        recordRepository.findByPromotionBatchId(batchId).forEach(r -> recordRepository.delete(r));
        Long orgId = currentOrgId();
        List<AcademicEnrollment> source = enrollmentRepository
                .findByOrganizationIdAndAcademicYearIdAndClassIdAndStatus(
                        orgId, b.getFromAcademicYearId(),
                        b.getFromClassId() != null ? b.getFromClassId() : 0L,
                        EnrollmentStatus.ACTIVE);
        List<PromotionRecord> records = new ArrayList<>();
        for (AcademicEnrollment e : source) {
            PromotionRecord r = PromotionRecord.builder()
                    .promotionBatchId(batchId)
                    .studentId(e.getStudentId())
                    .fromEnrollmentId(e.getId())
                    .decision(PromotionDecision.PROMOTED)
                    .build();
            records.add(recordRepository.save(r));
        }
        b.setPlannedCount(records.size());
        b.setProcessedCount(0);
        batchRepository.save(b);
        return records.stream().map(this::toDto).toList();
    }

    @Transactional
    public PromotionRecordDTO updateRecord(Long recordId, PromotionRecordDTO dto) {
        PromotionRecord r = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion record not found: " + recordId));
        PromotionBatch b = load(r.getPromotionBatchId());
        if (b.getStatus() != PromotionStatus.DRAFT) {
            throw new BadRequestException("Records can only be edited while batch is DRAFT");
        }
        if (dto.getDecision() != null) r.setDecision(dto.getDecision());
        if (dto.getReason() != null) r.setReason(dto.getReason());
        r = recordRepository.save(r);
        return toDto(r);
    }

    @Transactional
    public PromotionBatchDTO execute(Long batchId) {
        PromotionBatch b = load(batchId);
        if (b.getStatus() != PromotionStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT batches can be executed");
        }
        Long orgId = currentOrgId();
        List<PromotionRecord> records = recordRepository.findByPromotionBatchId(batchId);
        if (records.isEmpty()) {
            throw new BadRequestException("No promotion records to execute. Run preview first.");
        }
        b.setStatus(PromotionStatus.IN_PROGRESS);
        batchRepository.save(b);

        int processed = 0;
        for (PromotionRecord r : records) {
            AcademicEnrollment from = enrollmentRepository.findById(r.getFromEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Source enrollment not found: " + r.getFromEnrollmentId()));
            switch (r.getDecision()) {
                case PROMOTED -> {
                    closeEnrollment(from, EnrollmentStatus.PROMOTED);
                    AcademicEnrollment to = AcademicEnrollment.builder()
                            .enrollmentNumber(from.getEnrollmentNumber())
                            .studentId(from.getStudentId())
                            .academicYearId(b.getToAcademicYearId())
                            .classId(b.getToClassId() != null ? b.getToClassId() : from.getClassId())
                            .sectionId(from.getSectionId())
                            .house(from.getHouse())
                            .enrollmentDate(LocalDate.now())
                            .status(EnrollmentStatus.ACTIVE)
                            .remarks("Promoted from batch " + b.getBatchCode())
                            .build();
                    to.setOrganizationId(orgId);
                    to = enrollmentRepository.save(to);
                    r.setToEnrollmentId(to.getId());
                }
                case GRADUATED -> closeEnrollment(from, EnrollmentStatus.GRADUATED);
                case TRANSFERRED_OUT -> closeEnrollment(from, EnrollmentStatus.TRANSFERRED_OUT);
                case RETAINED, WITHHELD -> {
                    // No change to source enrollment; student stays in same class.
                }
            }
            recordRepository.save(r);
            processed++;
        }
        b.setStatus(PromotionStatus.COMPLETED);
        b.setProcessedCount(processed);
        b.setExecutedOn(LocalDate.now());
        b = batchRepository.save(b);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "PROMOTION_BATCH_EXECUTE", "PromotionBatch",
                b.getId(), "Promotion batch " + b.getBatchCode() + " executed (" + processed + " records)");
        return toDto(b);
    }

    @Transactional
    public PromotionBatchDTO rollback(Long batchId) {
        PromotionBatch b = load(batchId);
        if (b.getStatus() != PromotionStatus.COMPLETED) {
            throw new BadRequestException("Only COMPLETED batches can be rolled back");
        }
        List<PromotionRecord> records = recordRepository.findByPromotionBatchId(batchId);
        for (PromotionRecord r : records) {
            if (r.getToEnrollmentId() != null) {
                enrollmentRepository.findById(r.getToEnrollmentId())
                        .ifPresent(enrollmentRepository::delete);
                r.setToEnrollmentId(null);
            }
            enrollmentRepository.findById(r.getFromEnrollmentId()).ifPresent(from -> {
                from.setStatus(EnrollmentStatus.ACTIVE);
                from.setExitDate(null);
                enrollmentRepository.save(from);
            });
            recordRepository.save(r);
        }
        b.setStatus(PromotionStatus.ROLLED_BACK);
        b = batchRepository.save(b);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "PROMOTION_BATCH_ROLLBACK", "PromotionBatch",
                b.getId(), "Promotion batch " + b.getBatchCode() + " rolled back");
        return toDto(b);
    }

    @Transactional
    public PromotionBatchDTO cancel(Long batchId) {
        PromotionBatch b = load(batchId);
        if (b.getStatus() != PromotionStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT batches can be cancelled");
        }
        b.setStatus(PromotionStatus.CANCELLED);
        b = batchRepository.save(b);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "PROMOTION_BATCH_CANCEL", "PromotionBatch",
                b.getId(), "Promotion batch " + b.getBatchCode() + " cancelled");
        return toDto(b);
    }

    private void closeEnrollment(AcademicEnrollment e, EnrollmentStatus status) {
        e.setStatus(status);
        e.setExitDate(LocalDate.now());
        enrollmentRepository.save(e);
    }

    private PromotionBatch load(Long id) {
        PromotionBatch b = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion batch not found: " + id));
        Long orgId = currentOrgId();
        if (orgId != null && !orgId.equals(b.getOrganizationId())) {
            throw new ResourceNotFoundException("Promotion batch not found: " + id);
        }
        return b;
    }

    private PromotionBatchDTO toDto(PromotionBatch b) {
        return PromotionBatchDTO.builder()
                .id(b.getId())
                .batchCode(b.getBatchCode())
                .fromAcademicYearId(b.getFromAcademicYearId())
                .toAcademicYearId(b.getToAcademicYearId())
                .fromClassId(b.getFromClassId())
                .toClassId(b.getToClassId())
                .status(b.getStatus())
                .plannedCount(b.getPlannedCount())
                .processedCount(b.getProcessedCount())
                .executedOn(b.getExecutedOn())
                .remarks(b.getRemarks())
                .build();
    }

    private PromotionRecordDTO toDto(PromotionRecord r) {
        return PromotionRecordDTO.builder()
                .id(r.getId())
                .promotionBatchId(r.getPromotionBatchId())
                .studentId(r.getStudentId())
                .fromEnrollmentId(r.getFromEnrollmentId())
                .toEnrollmentId(r.getToEnrollmentId())
                .decision(r.getDecision())
                .reason(r.getReason())
                .build();
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
