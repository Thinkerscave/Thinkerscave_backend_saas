package com.thinkerscave.common.fee.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ConflictException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.fee.domain.FeeContract;
import com.thinkerscave.common.fee.dto.FeeContractDTO;
import com.thinkerscave.common.fee.repository.FeeContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manages the per-enrollment {@link FeeContract} snapshot. A contract is the
 * authoritative billable agreement; invoices are generated against it.
 *
 * <p>{@code netPayable} is auto-derived if not supplied:
 * {@code annualAmount - discountAmount - scholarshipAmount}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeeContractService {

    private final FeeContractRepository feeContractRepository;
    private final FeeStructureService feeStructureService;
    private final AuditPublisher auditPublisher;

    public Page<FeeContractDTO> listByYear(Long academicYearId, Pageable pageable) {
        return feeContractRepository
                .findByOrganizationIdAndAcademicYearId(currentOrgId(), academicYearId, pageable)
                .map(this::toDto);
    }

    public List<FeeContractDTO> listByStudent(Long studentId) {
        return feeContractRepository.findByOrganizationIdAndStudentId(currentOrgId(), studentId)
                .stream().map(this::toDto).toList();
    }

    public FeeContractDTO getByEnrollment(Long enrollmentId) {
        return feeContractRepository.findByEnrollmentId(enrollmentId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fee contract not found for enrollment: " + enrollmentId));
    }

    @Transactional
    public FeeContractDTO save(FeeContractDTO dto) {
        Long orgId = currentOrgId();
        FeeContract entity;
        boolean creating = dto.getId() == null;
        if (creating) {
            feeContractRepository.findByEnrollmentId(dto.getEnrollmentId())
                    .ifPresent(e -> { throw new ConflictException("Fee contract already exists for enrollment " + dto.getEnrollmentId()); });
            entity = new FeeContract();
            entity.setOrganizationId(orgId);
        } else {
            entity = load(dto.getId());
        }
        entity.setEnrollmentId(dto.getEnrollmentId());
        entity.setStudentId(dto.getStudentId());
        entity.setFeeStructureId(dto.getFeeStructureId());
        entity.setAcademicYearId(dto.getAcademicYearId());

        BigDecimal annual = dto.getAnnualAmount() != null
                ? dto.getAnnualAmount()
                : feeStructureService.computeAnnualTotal(dto.getFeeStructureId());
        entity.setAnnualAmount(annual);

        BigDecimal discount = nz(dto.getDiscountAmount());
        BigDecimal scholarship = nz(dto.getScholarshipAmount());
        entity.setDiscountAmount(discount);
        entity.setScholarshipAmount(scholarship);
        entity.setNetPayable(dto.getNetPayable() != null
                ? dto.getNetPayable()
                : annual.subtract(discount).subtract(scholarship).max(BigDecimal.ZERO));

        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setRemarks(dto.getRemarks());

        FeeContract saved = feeContractRepository.save(entity);
        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "fee_contract.create" : "fee_contract.update",
                "FeeContract", saved.getId(), "Fee contract for enrollment " + saved.getEnrollmentId());
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        FeeContract entity = load(id);
        feeContractRepository.delete(entity);
        auditPublisher.publish(AuditEventType.DELETE, "fee_contract.delete",
                "FeeContract", id, "Deleted fee contract " + id);
    }

    // ---------------------------------------------------------- helpers ---

    FeeContract load(Long id) {
        return feeContractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee contract not found: " + id));
    }

    private Long currentOrgId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) throw new BadRequestException("No organization context");
        return orgId;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    FeeContractDTO toDto(FeeContract e) {
        return FeeContractDTO.builder()
                .id(e.getId())
                .enrollmentId(e.getEnrollmentId())
                .studentId(e.getStudentId())
                .feeStructureId(e.getFeeStructureId())
                .academicYearId(e.getAcademicYearId())
                .annualAmount(e.getAnnualAmount())
                .discountAmount(e.getDiscountAmount())
                .scholarshipAmount(e.getScholarshipAmount())
                .netPayable(e.getNetPayable())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .remarks(e.getRemarks())
                .build();
    }
}
