package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByStaffCode(String staffCode);

    // ─── Multi-tenant org-scoped queries ────────────────────────────────────────
    List<Staff> findByOrganizationId(Long organizationId);

    List<Staff> findByOrganizationIdAndIsActive(Long organizationId, Boolean isActive);

    Optional<Staff> findByStaffCodeAndOrganizationId(String staffCode, Long organizationId);
}
