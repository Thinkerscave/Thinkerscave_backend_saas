package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.AlumniStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlumniStaffRepository extends JpaRepository<AlumniStaff, Long> {

    List<AlumniStaff> findByOrganizationIdOrderByExitDateDesc(Long organizationId);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndExitType(Long organizationId, String exitType);
}
