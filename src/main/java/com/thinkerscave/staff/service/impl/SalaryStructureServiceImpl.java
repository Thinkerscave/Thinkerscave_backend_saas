package com.thinkerscave.staff.service.impl;

import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.dto.request.SalaryStructureRequest;
import com.thinkerscave.staff.dto.response.SalaryStructureResponse;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.entity.StaffSalaryStructure;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.staff.repository.StaffSalaryStructureRepository;
import com.thinkerscave.staff.service.SalaryStructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryStructureServiceImpl implements SalaryStructureService {

    private final StaffSalaryStructureRepository salaryStructureRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public Long createSalaryStructure(SalaryStructureRequest request) {
        Staff staff = getStaff(request.getStaffId());

        // Deactivate existing active salary structure
        salaryStructureRepository.findByStaff_StaffIdAndActiveTrue(staff.getStaffId())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    existing.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
                    salaryStructureRepository.save(existing);
                });

        StaffSalaryStructure structure = new StaffSalaryStructure();
        structure.setStaff(staff);
        mapRequest(request, structure);
        structure.setGrossSalary(calculateGross(structure));
        structure.setActive(true);

        StaffSalaryStructure saved = salaryStructureRepository.save(structure);
        log.info("Salary structure created: {} for staff: {}", saved.getSalaryStructureId(), staff.getStaffCode());
        return saved.getSalaryStructureId();
    }

    @Override
    @Transactional
    public void updateSalaryStructure(Long id, SalaryStructureRequest request) {
        StaffSalaryStructure structure = getEntity(id);
        mapRequest(request, structure);
        structure.setGrossSalary(calculateGross(structure));
        salaryStructureRepository.save(structure);
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryStructureResponse getCurrentSalaryStructure(Long staffId) {
        StaffSalaryStructure structure = salaryStructureRepository.findByStaff_StaffIdAndActiveTrue(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("No active salary structure for staff: " + staffId));
        return toResponse(structure);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryStructureResponse> getSalaryHistory(Long staffId) {
        if (!staffRepository.existsById(staffId)) {
            throw new ResourceNotFoundException("Staff not found: " + staffId);
        }
        return salaryStructureRepository.findByStaff_StaffIdOrderByEffectiveFromDesc(staffId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private StaffSalaryStructure getEntity(Long id) {
        return salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found: " + id));
    }

    private Staff getStaff(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));
    }

    private void mapRequest(SalaryStructureRequest req, StaffSalaryStructure s) {
        s.setSalaryType(req.getSalaryType());
        s.setBasicPay(orZero(req.getBasicPay()));
        s.setHra(orZero(req.getHra()));
        s.setDa(orZero(req.getDa()));
        s.setSpecialAllowance(orZero(req.getSpecialAllowance()));
        s.setTransportAllowance(orZero(req.getTransportAllowance()));
        s.setOtherAllowance(orZero(req.getOtherAllowance()));
        s.setPfEmployee(orZero(req.getPfEmployee()));
        s.setEsiEmployee(orZero(req.getEsiEmployee()));
        s.setProfessionalTax(orZero(req.getProfessionalTax()));
        s.setOtherDeduction(orZero(req.getOtherDeduction()));
        s.setBankName(req.getBankName());
        s.setAccountHolderName(req.getAccountHolderName());
        s.setAccountNumber(req.getAccountNumber());
        s.setIfscCode(req.getIfscCode());
        s.setEffectiveFrom(req.getEffectiveFrom());
    }

    private BigDecimal calculateGross(StaffSalaryStructure s) {
        return orZero(s.getBasicPay())
                .add(orZero(s.getHra()))
                .add(orZero(s.getDa()))
                .add(orZero(s.getSpecialAllowance()))
                .add(orZero(s.getTransportAllowance()))
                .add(orZero(s.getOtherAllowance()));
    }

    private BigDecimal calculateStatutoryDeductions(StaffSalaryStructure s) {
        return orZero(s.getPfEmployee())
                .add(orZero(s.getEsiEmployee()))
                .add(orZero(s.getProfessionalTax()))
                .add(orZero(s.getOtherDeduction()));
    }

    private BigDecimal orZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private SalaryStructureResponse toResponse(StaffSalaryStructure s) {
        return SalaryStructureResponse.builder()
                .salaryStructureId(s.getSalaryStructureId())
                .staffId(s.getStaff().getStaffId())
                .staffName(s.getStaff().getFirstName() + " " + s.getStaff().getLastName())
                .staffCode(s.getStaff().getStaffCode())
                .salaryType(s.getSalaryType())
                .basicPay(s.getBasicPay())
                .hra(s.getHra())
                .da(s.getDa())
                .specialAllowance(s.getSpecialAllowance())
                .transportAllowance(s.getTransportAllowance())
                .otherAllowance(s.getOtherAllowance())
                .pfEmployee(s.getPfEmployee())
                .esiEmployee(s.getEsiEmployee())
                .professionalTax(s.getProfessionalTax())
                .otherDeduction(s.getOtherDeduction())
                .totalStatutoryDeductions(calculateStatutoryDeductions(s))
                .grossSalary(s.getGrossSalary())
                .bankName(s.getBankName())
                .accountHolderName(s.getAccountHolderName())
                .accountNumber(s.getAccountNumber())
                .ifscCode(s.getIfscCode())
                .effectiveFrom(s.getEffectiveFrom())
                .effectiveTo(s.getEffectiveTo())
                .active(s.getActive())
                .createdOn(s.getCreatedOn())
                .updatedOn(s.getUpdatedOn())
                .build();
    }
}
