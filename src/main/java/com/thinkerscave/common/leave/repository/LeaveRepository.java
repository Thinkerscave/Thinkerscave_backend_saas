package com.thinkerscave.common.leave.repository;

import com.thinkerscave.common.leave.domain.LeaveRequest;
import com.thinkerscave.common.leave.domain.LeaveRequest.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Queries run in the CURRENT TENANT SCHEMA (set by TenantFilter + Hibernate
 * schema routing).
 * All methods include organizationId to enforce branch-level data isolation
 * within a tenant.
 */
@Repository
public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByOrganizationIdOrderByCreatedDateDesc(Long organizationId);

    List<LeaveRequest> findByOrganizationIdAndAppliedByOrderByCreatedDateDesc(
            Long organizationId, String appliedBy);

    List<LeaveRequest> findByOrganizationIdAndStatus(Long organizationId, LeaveStatus status);

    List<LeaveRequest> findByOrganizationIdAndStaffId(Long organizationId, Long staffId);
}
