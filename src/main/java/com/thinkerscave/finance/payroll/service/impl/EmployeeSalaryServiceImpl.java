package com.thinkerscave.finance.payroll.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.payroll.dto.request.EmployeeSalaryRequest;
import com.thinkerscave.finance.payroll.dto.response.EmployeeSalaryResponse;
import com.thinkerscave.finance.payroll.entity.EmployeeSalary;
import com.thinkerscave.finance.payroll.entity.EmployeeSalaryComponent;
import com.thinkerscave.finance.payroll.entity.SalaryComponent;
import com.thinkerscave.finance.payroll.entity.SalaryStructureItem;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import com.thinkerscave.finance.payroll.repository.EmployeeSalaryComponentRepository;
import com.thinkerscave.finance.payroll.repository.EmployeeSalaryRepository;
import com.thinkerscave.finance.payroll.repository.SalaryComponentRepository;
import com.thinkerscave.finance.payroll.repository.SalaryStructureItemRepository;
import com.thinkerscave.finance.payroll.repository.SalaryStructureRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.finance.payroll.service.EmployeeSalaryService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeSalaryServiceImpl implements EmployeeSalaryService {

    private final EmployeeSalaryRepository employeeSalaryRepository;
    private final EmployeeSalaryComponentRepository componentRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final SalaryStructureItemRepository structureItemRepository;
    private final StaffRepository staffRepository;
    private final PayrollAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    public EmployeeSalaryResponse getCurrent(Long staffId) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_EMPLOYEE_SALARY);
        requireStaff(staffId);
        EmployeeSalary salary = employeeSalaryRepository
                .findFirstByStaffIdAndActiveTrueOrderByEffectiveFromDescEmployeeSalaryIdDesc(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("No active employee salary for staff " + staffId));
        return toResponse(salary);
    }

    @Override
    public List<EmployeeSalaryResponse> history(Long staffId) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_EMPLOYEE_SALARY);
        requireStaff(staffId);
        return employeeSalaryRepository.findByStaffIdOrderByEffectiveFromDesc(staffId).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public EmployeeSalaryResponse assign(Long staffId, EmployeeSalaryRequest request) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_EMPLOYEE_SALARY);
        requireStaff(staffId);
        if (request.getEffectiveFrom() == null) {
            throw new BadRequestException("effectiveFrom is required");
        }
        for (EmployeeSalary prior : employeeSalaryRepository.findByStaffIdAndActiveTrueOrderByEffectiveFromDesc(staffId)) {
            prior.setActive(false);
            LocalDate closeTo = request.getEffectiveFrom().minusDays(1);
            if (prior.getEffectiveTo() == null || prior.getEffectiveTo().isAfter(closeTo)) {
                prior.setEffectiveTo(closeTo);
            }
            employeeSalaryRepository.save(prior);
        }

        EmployeeSalary salary = new EmployeeSalary();
        salary.setStaffId(staffId);
        salary.setPaymentType(request.getPaymentType() != null ? request.getPaymentType() : PaymentType.SALARY);
        salary.setSalaryStructureId(request.getSalaryStructureId());
        salary.setEffectiveFrom(request.getEffectiveFrom());
        salary.setEffectiveTo(null);
        salary.setActive(true);
        salary.setBankName(request.getBankName());
        salary.setAccountHolderName(request.getAccountHolderName());
        salary.setAccountNumber(request.getAccountNumber());
        salary.setIfscCode(request.getIfscCode());
        salary.setRemarks(request.getRemarks());
        EmployeeSalary saved = employeeSalaryRepository.save(salary);

        List<EmployeeSalaryRequest.ComponentLine> lines = request.getComponents();
        if ((lines == null || lines.isEmpty()) && request.getSalaryStructureId() != null) {
            salaryStructureRepository.findById(request.getSalaryStructureId())
                    .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found"));
            lines = structureItemRepository
                    .findBySalaryStructure_SalaryStructureIdOrderBySalaryStructureItemIdAsc(request.getSalaryStructureId())
                    .stream()
                    .map(this::fromStructureItem)
                    .toList();
        }
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("At least one salary component line is required");
        }
        List<EmployeeSalaryComponent> toSave = new ArrayList<>();
        for (EmployeeSalaryRequest.ComponentLine line : lines) {
            SalaryComponent component = salaryComponentRepository.findById(line.getSalaryComponentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Salary component not found: " + line.getSalaryComponentId()));
            EmployeeSalaryComponent row = new EmployeeSalaryComponent();
            row.setEmployeeSalary(saved);
            row.setSalaryComponent(component);
            row.setComponentType(line.getComponentType() != null ? line.getComponentType() : component.getComponentType());
            row.setCalculationMethod(line.getCalculationMethod());
            row.setValue(line.getValue());
            boolean applicable = line.getApplicable() == null || line.getApplicable();
            // Stipend: default statutory components off unless explicitly applicable
            if (saved.getPaymentType() == PaymentType.STIPEND
                    && component.getStatutoryCode() != null
                    && line.getApplicable() == null) {
                applicable = false;
            }
            row.setApplicable(applicable);
            toSave.add(row);
        }
        componentRepository.saveAll(toSave);

        auditWriteService.record(AuditEventType.CREATE, "EMPLOYEE_SALARY_ASSIGN", "EmployeeSalary",
                String.valueOf(saved.getEmployeeSalaryId()), "Assigned employee salary for staff " + staffId);
        return toResponse(saved);
    }

    private EmployeeSalaryRequest.ComponentLine fromStructureItem(SalaryStructureItem item) {
        EmployeeSalaryRequest.ComponentLine line = new EmployeeSalaryRequest.ComponentLine();
        line.setSalaryComponentId(item.getSalaryComponent().getSalaryComponentId());
        line.setComponentType(item.getSalaryComponent().getComponentType());
        line.setCalculationMethod(item.getCalculationMethod());
        line.setValue(item.getValue());
        line.setApplicable(true);
        return line;
    }

    private void requireStaff(Long staffId) {
        if (!staffRepository.existsById(staffId)) {
            throw new ResourceNotFoundException("Staff not found: " + staffId);
        }
    }

    private EmployeeSalaryResponse toResponse(EmployeeSalary salary) {
        List<EmployeeSalaryResponse.ComponentLine> components = componentRepository
                .findByEmployeeSalary_EmployeeSalaryId(salary.getEmployeeSalaryId())
                .stream()
                .map(c -> EmployeeSalaryResponse.ComponentLine.builder()
                        .employeeSalaryComponentId(c.getEmployeeSalaryComponentId())
                        .salaryComponentId(c.getSalaryComponent().getSalaryComponentId())
                        .componentCode(c.getSalaryComponent().getCode())
                        .componentName(c.getSalaryComponent().getName())
                        .componentType(c.getComponentType())
                        .calculationMethod(c.getCalculationMethod())
                        .value(c.getValue())
                        .applicable(c.getApplicable())
                        .build())
                .toList();
        return EmployeeSalaryResponse.builder()
                .employeeSalaryId(salary.getEmployeeSalaryId())
                .staffId(salary.getStaffId())
                .paymentType(salary.getPaymentType())
                .salaryStructureId(salary.getSalaryStructureId())
                .effectiveFrom(salary.getEffectiveFrom())
                .effectiveTo(salary.getEffectiveTo())
                .active(salary.getActive())
                .bankName(salary.getBankName())
                .accountHolderName(salary.getAccountHolderName())
                .accountNumber(salary.getAccountNumber())
                .ifscCode(salary.getIfscCode())
                .remarks(salary.getRemarks())
                .components(components)
                .build();
    }
}
