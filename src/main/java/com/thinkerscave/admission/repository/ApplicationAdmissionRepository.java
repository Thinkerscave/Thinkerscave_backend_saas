package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.ApplicationAdmission;
import com.thinkerscave.admission.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationAdmissionRepository extends JpaRepository<ApplicationAdmission, Long> {

    Optional<ApplicationAdmission> findByApplicationNumberAndOrganizationId(String appNumber, Long orgId);

    Page<ApplicationAdmission> findByOrganizationIdOrderByCreatedOnDesc(Long orgId, Pageable pageable);

    Page<ApplicationAdmission> findByOrganizationIdAndStatusOrderByCreatedOnDesc(
            Long orgId, ApplicationStatus status, Pageable pageable);

    long countByOrganizationIdAndStatus(Long orgId, ApplicationStatus status);

    boolean existsByApplicationNumber(String applicationNumber);
}
