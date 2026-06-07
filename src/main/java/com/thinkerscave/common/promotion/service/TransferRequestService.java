package com.thinkerscave.common.promotion.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.common.sequence.SequenceGeneratorService;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.enrollment.domain.AcademicEnrollment;
import com.thinkerscave.common.enrollment.domain.EnrollmentStatus;
import com.thinkerscave.common.enrollment.repository.AcademicEnrollmentRepository;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.promotion.domain.TransferRequest;
import com.thinkerscave.common.promotion.domain.TransferStatus;
import com.thinkerscave.common.promotion.dto.TransferRequestDTO;
import com.thinkerscave.common.promotion.repository.TransferRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Student transfer / school leaving certificate workflow.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TransferRequestService {

    private static final Set<TransferStatus> TERMINAL = EnumSet.of(
            TransferStatus.REJECTED, TransferStatus.CERTIFICATE_ISSUED, TransferStatus.CANCELLED);

    private final TransferRequestRepository transferRepository;
    private final AcademicEnrollmentRepository enrollmentRepository;
    private final SequenceGeneratorService sequenceGenerator;
    private final AuditPublisher auditPublisher;

    public Page<TransferRequestDTO> list(Pageable pageable) {
        return transferRepository.findByOrganizationId(currentOrgId(), pageable).map(this::toDto);
    }

    public List<TransferRequestDTO> listForStudent(Long studentId) {
        return transferRepository.findByOrganizationIdAndStudentId(currentOrgId(), studentId)
                .stream().map(this::toDto).toList();
    }

    public TransferRequestDTO get(Long id) {
        return toDto(load(id));
    }

    @Transactional
    public TransferRequestDTO create(TransferRequestDTO dto) {
        if (dto.getStudentId() == null) {
            throw new BadRequestException("studentId is required");
        }
        if (dto.getReason() == null || dto.getReason().isBlank()) {
            throw new BadRequestException("reason is required");
        }
        Long orgId = currentOrgId();
        Long enrollmentId = resolveEnrollmentId(dto, orgId);
        String number = dto.getRequestNumber();
        if (number == null || number.isBlank()) {
            number = sequenceGenerator.nextNumber(orgId, "TRANSFER", null);
        }
        TransferRequest t = TransferRequest.builder()
                .requestNumber(number)
                .studentId(dto.getStudentId())
                .enrollmentId(enrollmentId)
                .requestedOn(dto.getRequestedOn() != null ? dto.getRequestedOn() : LocalDate.now())
                .reason(dto.getReason())
                .destinationSchool(dto.getDestinationSchool())
                .status(TransferStatus.REQUESTED)
                .remarks(dto.getRemarks())
                .build();
        t.setOrganizationId(orgId);
        t = transferRepository.save(t);
        auditPublisher.publish(AuditEventType.CREATE, "TRANSFER_REQUEST_CREATE", "TransferRequest",
                t.getId(), "Transfer request " + t.getRequestNumber() + " created");
        return toDto(t);
    }

    private Long resolveEnrollmentId(TransferRequestDTO dto, Long orgId) {
        if (dto.getEnrollmentId() != null) {
            AcademicEnrollment enrollment = enrollmentRepository.findById(dto.getEnrollmentId())
                    .orElseThrow(() -> new BadRequestException("enrollmentId is invalid"));
            if (!dto.getStudentId().equals(enrollment.getStudentId())
                    || (orgId != null && !orgId.equals(enrollment.getOrganizationId()))) {
                throw new BadRequestException("enrollmentId does not belong to the selected student");
            }
            return enrollment.getId();
        }
        return enrollmentRepository
                .findFirstByOrganizationIdAndStudentIdAndStatusOrderByEnrollmentDateDesc(
                        orgId, dto.getStudentId(), EnrollmentStatus.ACTIVE)
                .map(AcademicEnrollment::getId)
                .orElseThrow(() -> new BadRequestException("No active enrollment found for selected student"));
    }

    @Transactional
    public TransferRequestDTO transition(Long id, TransferStatus target, Long actorUserId, String remarks) {
        if (target == null) throw new BadRequestException("Target status is required");
        TransferRequest t = load(id);
        if (TERMINAL.contains(t.getStatus())) {
            throw new BadRequestException("Transfer request is in terminal state: " + t.getStatus());
        }
        TransferStatus prev = t.getStatus();
        t.setStatus(target);
        if (target == TransferStatus.APPROVED) {
            t.setApprovedByUserId(actorUserId);
            t.setApprovedOn(LocalDate.now());
        } else if (target == TransferStatus.CERTIFICATE_ISSUED) {
            if (t.getCertificateNumber() == null || t.getCertificateNumber().isBlank()) {
                t.setCertificateNumber(sequenceGenerator.nextNumber(currentOrgId(), "TC", null));
            }
            t.setCertificateIssuedOn(LocalDate.now());
            enrollmentRepository.findById(t.getEnrollmentId()).ifPresent(en -> {
                en.setStatus(EnrollmentStatus.TRANSFERRED_OUT);
                if (en.getExitDate() == null) en.setExitDate(LocalDate.now());
                enrollmentRepository.save(en);
            });
        }
        if (remarks != null && !remarks.isBlank()) t.setRemarks(remarks);
        t = transferRepository.save(t);
        AuditEventType eventType = switch (target) {
            case APPROVED -> AuditEventType.APPROVAL;
            case REJECTED -> AuditEventType.REJECTION;
            default -> AuditEventType.STATE_CHANGE;
        };
        auditPublisher.publish(eventType, "TRANSFER_REQUEST_TRANSITION", "TransferRequest",
                t.getId(), "Transfer " + t.getRequestNumber() + " " + prev + " -> " + target);
        return toDto(t);
    }

    private TransferRequest load(Long id) {
        TransferRequest t = transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer request not found: " + id));
        Long orgId = currentOrgId();
        if (orgId != null && !orgId.equals(t.getOrganizationId())) {
            throw new ResourceNotFoundException("Transfer request not found: " + id);
        }
        return t;
    }

    private TransferRequestDTO toDto(TransferRequest t) {
        return TransferRequestDTO.builder()
                .id(t.getId())
                .requestNumber(t.getRequestNumber())
                .studentId(t.getStudentId())
                .enrollmentId(t.getEnrollmentId())
                .requestedOn(t.getRequestedOn())
                .reason(t.getReason())
                .destinationSchool(t.getDestinationSchool())
                .status(t.getStatus())
                .approvedByUserId(t.getApprovedByUserId())
                .approvedOn(t.getApprovedOn())
                .certificateNumber(t.getCertificateNumber())
                .certificateIssuedOn(t.getCertificateIssuedOn())
                .remarks(t.getRemarks())
                .build();
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
