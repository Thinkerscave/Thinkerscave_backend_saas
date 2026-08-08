package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.ApplicationAdmission;
import com.thinkerscave.admission.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationAdmissionRepository extends JpaRepository<ApplicationAdmission, Long>, JpaSpecificationExecutor<ApplicationAdmission> {

    Optional<ApplicationAdmission> findByApplicationNumber(String appNumber);

    Page<ApplicationAdmission> findByOrderByCreatedOnDesc(Pageable pageable);

    Page<ApplicationAdmission> findByStatusOrderByCreatedOnDesc(ApplicationStatus status, Pageable pageable);

    long countByStatus(ApplicationStatus status);

    boolean existsByInquiryId(Long inquiryId);

    Optional<ApplicationAdmission> findByInquiryId(Long inquiryId);

    boolean existsByApplicationNumber(String applicationNumber);
}
