package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.StaffResponsibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffResponsibilityRepository extends JpaRepository<StaffResponsibility, Long> {

    List<StaffResponsibility> findByOrganizationIdOrderByCreatedDateDesc(Long organizationId);

    List<StaffResponsibility> findByOrganizationIdAndStaffIdOrderByCreatedDateDesc(Long organizationId, Long staffId);

    Optional<StaffResponsibility> findByResponsibilityIdAndOrganizationId(Long responsibilityId, Long organizationId);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, String status);

    long countByOrganizationIdAndStaffIdIsNull(Long organizationId);
}
