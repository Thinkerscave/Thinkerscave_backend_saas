package com.thinkerscave.finance.payroll.repository;

import com.thinkerscave.finance.payroll.entity.EmployeeSalaryComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeSalaryComponentRepository extends JpaRepository<EmployeeSalaryComponent, Long> {
    List<EmployeeSalaryComponent> findByEmployeeSalary_EmployeeSalaryId(Long employeeSalaryId);
    void deleteByEmployeeSalary_EmployeeSalaryId(Long employeeSalaryId);
    boolean existsBySalaryComponent_SalaryComponentId(Long componentId);
}
