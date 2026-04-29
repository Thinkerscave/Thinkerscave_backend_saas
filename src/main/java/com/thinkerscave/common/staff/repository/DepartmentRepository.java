package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByDepartmentCode(String departmentCode);

    List<Department> findAllByIsActiveTrue();

    // Multi-tenant: find departments by org
    List<Department> findByOrganizationIdAndIsActive(Long organizationId, Boolean isActive);
}
