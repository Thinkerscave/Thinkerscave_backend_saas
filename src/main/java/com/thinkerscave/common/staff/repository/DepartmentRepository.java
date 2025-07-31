package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department,Long> {

    Optional<Department> findByDepartmentCode(String departmentCode);

    List<Department> findAllByIsActiveTrue();


}
