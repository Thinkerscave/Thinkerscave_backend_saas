package com.thinkerscave.common.staff.service.impl;

import org.springframework.transaction.annotation.Transactional;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.staff.domain.Department;
import com.thinkerscave.common.staff.repository.DepartmentRepository;
import com.thinkerscave.common.staff.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    public Map<String, Object> getAllActiveDepartment() {
        Map<String, Object> data = new HashMap<>();
        try {
            Long orgId = OrganizationContext.getOrganizationId();
            List<Department> list = (orgId != null)
                    ? departmentRepository.findByOrganizationIdAndIsActive(orgId, true)
                    : departmentRepository.findAllByIsActiveTrue();
            if (!list.isEmpty()) {
                data.put("isOutcome", true);
                data.put("message", "All Department Records Fetched");
                data.put("data", list);
            } else {
                data.put("isOutcome", false);
                data.put("message", "No active departments found");
            }
        } catch (Exception e) {
            log.error("Exception while fetching departments", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error: " + e.getMessage());
        }
        return data;
    }

    @Override
    public Map<String, Object> saveOrUpdate(Department department) {
        Map<String, Object> data = new HashMap<>();
        try {
            if (department.getId() == null) {
                Long orgId = OrganizationContext.getOrganizationId();
                if (orgId != null)
                    department.setOrganizationId(orgId);
                if (department.getIsActive() == null)
                    department.setIsActive(true);
            }
            Department saved = departmentRepository.save(department);
            data.put("isOutcome", true);
            data.put("message", department.getId() == null ? "Department created" : "Department updated");
            data.put("data", saved);
        } catch (Exception e) {
            log.error("Exception while saving department", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error: " + e.getMessage());
        }
        return data;
    }

    @Override
    public Map<String, Object> toggleActive(Long id) {
        Map<String, Object> data = new HashMap<>();
        try {
            Department dept = departmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Department not found: " + id));
            dept.setIsActive(!dept.getIsActive());
            departmentRepository.save(dept);
            data.put("isOutcome", true);
            data.put("message", "Department " + (dept.getIsActive() ? "activated" : "deactivated"));
        } catch (Exception e) {
            log.error("Exception while toggling department status", e);
            data.put("isOutcome", false);
            data.put("message", "Unexpected error: " + e.getMessage());
        }
        return data;
    }
}
