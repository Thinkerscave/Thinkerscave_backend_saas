package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.Staff;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    @EntityGraph(attributePaths = { "user", "branch", "department" })
    Optional<Staff> findByStaffCode(String staffCode);

    // ─── Multi-tenant org-scoped queries ────────────────────────────────────────
    @EntityGraph(attributePaths = { "user", "branch", "department" })
    List<Staff> findByOrganizationId(Long organizationId);

    @EntityGraph(attributePaths = { "user", "branch", "department" })
    List<Staff> findByOrganizationIdAndIsActive(Long organizationId, Boolean isActive);

    @EntityGraph(attributePaths = { "user", "branch", "department" })
    Optional<Staff> findByStaffCodeAndOrganizationId(String staffCode, Long organizationId);
}
