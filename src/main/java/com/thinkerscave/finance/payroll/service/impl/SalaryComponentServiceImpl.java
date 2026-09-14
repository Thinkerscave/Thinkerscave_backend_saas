package com.thinkerscave.finance.payroll.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.payroll.dto.request.SalaryComponentRequest;
import com.thinkerscave.finance.payroll.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.payroll.dto.response.SalaryComponentResponse;
import com.thinkerscave.finance.payroll.entity.SalaryComponent;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.repository.EmployeeSalaryComponentRepository;
import com.thinkerscave.finance.payroll.repository.SalaryComponentRepository;
import com.thinkerscave.finance.payroll.repository.SalaryStructureItemRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.finance.payroll.service.SalaryComponentService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalaryComponentServiceImpl implements SalaryComponentService {

    private final SalaryComponentRepository repository;
    private final SalaryStructureItemRepository structureItemRepository;
    private final EmployeeSalaryComponentRepository employeeSalaryComponentRepository;
    private final PayrollAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    public PageResponse<SalaryComponentResponse> list(String q, ComponentType type, ComponentStatus status, Pageable pageable) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_COMPONENTS);
        String qq = blankToNull(q);
        if (qq == null) {
            if (type == null && status == null) {
                return PageResponse.of(repository.findAll(pageable), this::toResponse);
            }
            return PageResponse.of(repository.filter(type, status, pageable), this::toResponse);
        }
        return PageResponse.of(repository.search(qq, type, status, pageable), this::toResponse);
    }

    @Override
    public List<SalaryComponentResponse> lookups() {
        if (!accessGuard.canView(PayrollAccessGuard.RESOURCE_COMPONENTS)
                && !accessGuard.canView(PayrollAccessGuard.RESOURCE_STRUCTURES)
                && !accessGuard.canView(PayrollAccessGuard.RESOURCE_EMPLOYEE_SALARY)) {
            accessGuard.requireView(PayrollAccessGuard.RESOURCE_COMPONENTS);
        }
        return repository.findByStatusOrderBySortOrderAscNameAsc(ComponentStatus.ACTIVE).stream()
                .map(this::toResponse).toList();
    }

    @Override
    public SalaryComponentResponse get(Long id) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_COMPONENTS);
        return toResponse(require(id));
    }

    @Override
    @Transactional
    public SalaryComponentResponse create(SalaryComponentRequest request) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_COMPONENTS);
        String code = normalizeCode(request.getCode());
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new AlreadyExistsException("Salary component code already exists", "code");
        }
        SalaryComponent entity = new SalaryComponent();
        apply(entity, request, code);
        if (entity.getStatus() == null) {
            entity.setStatus(ComponentStatus.ACTIVE);
        }
        SalaryComponent saved = repository.save(entity);
        auditWriteService.record(AuditEventType.CREATE, "SALARY_COMPONENT_CREATE", "SalaryComponent",
                String.valueOf(saved.getSalaryComponentId()), "Created salary component " + saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SalaryComponentResponse update(Long id, SalaryComponentRequest request) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_COMPONENTS);
        SalaryComponent entity = require(id);
        String code = normalizeCode(request.getCode());
        repository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getSalaryComponentId().equals(id)) {
                throw new AlreadyExistsException("Salary component code already exists", "code");
            }
        });
        apply(entity, request, code);
        SalaryComponent saved = repository.save(entity);
        auditWriteService.record(AuditEventType.UPDATE, "SALARY_COMPONENT_UPDATE", "SalaryComponent",
                String.valueOf(id), "Updated salary component");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SalaryComponentResponse updateStatus(Long id, StatusUpdateRequest request) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_COMPONENTS);
        SalaryComponent entity = require(id);
        entity.setStatus(request.getStatus());
        SalaryComponent saved = repository.save(entity);
        auditWriteService.record(AuditEventType.UPDATE, "SALARY_COMPONENT_STATUS", "SalaryComponent",
                String.valueOf(id), "Status " + request.getStatus());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_COMPONENTS);
        SalaryComponent entity = require(id);
        if (structureItemRepository.existsBySalaryComponent_SalaryComponentId(id)
                || employeeSalaryComponentRepository.existsBySalaryComponent_SalaryComponentId(id)) {
            entity.setStatus(ComponentStatus.INACTIVE);
            repository.save(entity);
            auditWriteService.record(AuditEventType.UPDATE, "SALARY_COMPONENT_DEACTIVATE", "SalaryComponent",
                    String.valueOf(id), "Deactivated in-use salary component");
            return;
        }
        repository.delete(entity);
        auditWriteService.record(AuditEventType.DELETE, "SALARY_COMPONENT_DELETE", "SalaryComponent",
                String.valueOf(id), "Deleted salary component");
    }

    private void apply(SalaryComponent entity, SalaryComponentRequest request, String code) {
        entity.setCode(code);
        entity.setName(request.getName().trim());
        entity.setComponentType(request.getComponentType());
        entity.setCalculationMethod(request.getCalculationMethod());
        entity.setDefaultValue(request.getDefaultValue());
        entity.setStatutoryCode(request.getStatutoryCode());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
    }

    private SalaryComponent require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary component not found: " + id));
    }

    private SalaryComponentResponse toResponse(SalaryComponent c) {
        return SalaryComponentResponse.builder()
                .salaryComponentId(c.getSalaryComponentId())
                .code(c.getCode())
                .name(c.getName())
                .componentType(c.getComponentType())
                .calculationMethod(c.getCalculationMethod())
                .defaultValue(c.getDefaultValue())
                .statutoryCode(c.getStatutoryCode())
                .status(c.getStatus())
                .sortOrder(c.getSortOrder())
                .build();
    }

    private static String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Component code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    private static String blankToNull(String q) {
        return StringUtils.hasText(q) ? q.trim() : null;
    }
}
