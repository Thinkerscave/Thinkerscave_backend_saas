package com.thinkerscave.finance.payroll.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.payroll.dto.request.SalaryStructureRequest;
import com.thinkerscave.finance.payroll.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.payroll.dto.response.SalaryStructureResponse;
import com.thinkerscave.finance.payroll.entity.SalaryComponent;
import com.thinkerscave.finance.payroll.entity.SalaryStructure;
import com.thinkerscave.finance.payroll.entity.SalaryStructureItem;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import com.thinkerscave.finance.payroll.repository.SalaryComponentRepository;
import com.thinkerscave.finance.payroll.repository.SalaryStructureItemRepository;
import com.thinkerscave.finance.payroll.repository.SalaryStructureRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.finance.payroll.service.SalaryStructureService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalaryStructureServiceImpl implements SalaryStructureService {

    private final SalaryStructureRepository structureRepository;
    private final SalaryStructureItemRepository itemRepository;
    private final SalaryComponentRepository componentRepository;
    private final PayrollAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    public PageResponse<SalaryStructureResponse> list(String q, ComponentStatus status, Pageable pageable) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_STRUCTURES);
        String qq = StringUtils.hasText(q) ? q.trim() : null;
        if (qq == null) {
            return PageResponse.of(structureRepository.filter(status, pageable), this::toResponse);
        }
        return PageResponse.of(structureRepository.search(qq, status, pageable), this::toResponse);
    }

    @Override
    public List<SalaryStructureResponse> lookups() {
        if (!accessGuard.canView(PayrollAccessGuard.RESOURCE_STRUCTURES)
                && !accessGuard.canView(PayrollAccessGuard.RESOURCE_EMPLOYEE_SALARY)) {
            accessGuard.requireView(PayrollAccessGuard.RESOURCE_STRUCTURES);
        }
        return structureRepository.findByStatusOrderByNameAsc(ComponentStatus.ACTIVE).stream()
                .map(this::toResponse).toList();
    }

    @Override
    public SalaryStructureResponse get(Long id) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_STRUCTURES);
        return toResponse(require(id));
    }

    @Override
    @Transactional
    public SalaryStructureResponse create(SalaryStructureRequest request) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_STRUCTURES);
        SalaryStructure structure = new SalaryStructure();
        applyHeader(structure, request);
        if (structure.getStatus() == null) {
            structure.setStatus(ComponentStatus.ACTIVE);
        }
        SalaryStructure saved = structureRepository.save(structure);
        replaceItems(saved, request.getItems());
        auditWriteService.record(AuditEventType.CREATE, "SALARY_STRUCTURE_CREATE", "SalaryStructure",
                String.valueOf(saved.getSalaryStructureId()), "Created salary structure " + saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SalaryStructureResponse update(Long id, SalaryStructureRequest request) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_STRUCTURES);
        SalaryStructure structure = require(id);
        applyHeader(structure, request);
        SalaryStructure saved = structureRepository.save(structure);
        replaceItems(saved, request.getItems());
        auditWriteService.record(AuditEventType.UPDATE, "SALARY_STRUCTURE_UPDATE", "SalaryStructure",
                String.valueOf(id), "Updated salary structure");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SalaryStructureResponse updateStatus(Long id, StatusUpdateRequest request) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_STRUCTURES);
        SalaryStructure structure = require(id);
        structure.setStatus(request.getStatus());
        SalaryStructure saved = structureRepository.save(structure);
        auditWriteService.record(AuditEventType.UPDATE, "SALARY_STRUCTURE_STATUS", "SalaryStructure",
                String.valueOf(id), "Status " + request.getStatus());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_STRUCTURES);
        SalaryStructure structure = require(id);
        structure.setStatus(ComponentStatus.INACTIVE);
        structureRepository.save(structure);
        auditWriteService.record(AuditEventType.UPDATE, "SALARY_STRUCTURE_DEACTIVATE", "SalaryStructure",
                String.valueOf(id), "Deactivated salary structure");
    }

    private void replaceItems(SalaryStructure structure, List<SalaryStructureRequest.Item> items) {
        itemRepository.deleteBySalaryStructure_SalaryStructureId(structure.getSalaryStructureId());
        if (items == null || items.isEmpty()) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        List<SalaryStructureItem> toSave = new ArrayList<>();
        for (SalaryStructureRequest.Item item : items) {
            if (!seen.add(item.getSalaryComponentId())) {
                throw new BadRequestException("Duplicate component in structure");
            }
            SalaryComponent component = componentRepository.findById(item.getSalaryComponentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Salary component not found: " + item.getSalaryComponentId()));
            SalaryStructureItem row = new SalaryStructureItem();
            row.setSalaryStructure(structure);
            row.setSalaryComponent(component);
            row.setCalculationMethod(item.getCalculationMethod());
            row.setValue(item.getValue());
            toSave.add(row);
        }
        itemRepository.saveAll(toSave);
    }

    private void applyHeader(SalaryStructure structure, SalaryStructureRequest request) {
        structure.setName(request.getName().trim());
        structure.setDescription(request.getDescription());
        structure.setApplicableStaffType(request.getApplicableStaffType());
        if (request.getStatus() != null) {
            structure.setStatus(request.getStatus());
        }
    }

    private SalaryStructure require(Long id) {
        return structureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found: " + id));
    }

    private SalaryStructureResponse toResponse(SalaryStructure structure) {
        List<SalaryStructureResponse.Item> items = itemRepository
                .findBySalaryStructure_SalaryStructureIdOrderBySalaryStructureItemIdAsc(structure.getSalaryStructureId())
                .stream()
                .map(i -> SalaryStructureResponse.Item.builder()
                        .salaryStructureItemId(i.getSalaryStructureItemId())
                        .salaryComponentId(i.getSalaryComponent().getSalaryComponentId())
                        .componentCode(i.getSalaryComponent().getCode())
                        .componentName(i.getSalaryComponent().getName())
                        .calculationMethod(i.getCalculationMethod())
                        .value(i.getValue())
                        .build())
                .toList();
        return SalaryStructureResponse.builder()
                .salaryStructureId(structure.getSalaryStructureId())
                .name(structure.getName())
                .description(structure.getDescription())
                .applicableStaffType(structure.getApplicableStaffType())
                .status(structure.getStatus())
                .items(items)
                .build();
    }
}
