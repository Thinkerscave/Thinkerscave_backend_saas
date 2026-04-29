package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchCode(String branchCode);

    List<Branch> findAllByIsActiveTrue();

    // Multi-tenant: find branches by org
    List<Branch> findByOrganizationIdAndIsActive(Long organizationId, Boolean isActive);
}
