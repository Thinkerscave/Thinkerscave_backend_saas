package com.thinkerscave.common.staff.service;

import com.thinkerscave.common.staff.domain.Department;
import java.util.Map;

public interface DepartmentService {
    Map<String, Object> getAllActiveDepartment();

    Map<String, Object> saveOrUpdate(Department department);

    Map<String, Object> toggleActive(Long id);
}
