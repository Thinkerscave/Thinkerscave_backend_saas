package com.thinkerscave.common.payroll.repository;

import com.thinkerscave.common.payroll.domain.StaffPayroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Queries run in the CURRENT TENANT SCHEMA (set by TenantFilter + Hibernate
 * schema routing).
 * All methods include organizationId to enforce branch-level data isolation
 * within a tenant.
 */
@Repository
public interface PayrollRepository extends JpaRepository<StaffPayroll, Long> {

    List<StaffPayroll> findByOrganizationId(Long organizationId);

    Optional<StaffPayroll> findByOrganizationIdAndStaffId(Long organizationId, Long staffId);
}
