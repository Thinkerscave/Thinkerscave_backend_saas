package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
}
