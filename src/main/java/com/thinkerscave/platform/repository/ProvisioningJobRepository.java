package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.ProvisioningJob;
import com.thinkerscave.platform.enums.ProvisionJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProvisioningJobRepository extends JpaRepository<ProvisioningJob, Long> {

    Optional<ProvisioningJob> findByJobCode(String jobCode);

    List<ProvisioningJob> findByOrganization_IdOrderByCreatedOnDesc(Long organizationId);

    @Query("""
            SELECT j FROM ProvisioningJob j
            WHERE j.active = true
            AND (:status IS NULL OR j.status = :status)
            AND (:search IS NULL OR LOWER(j.jobCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(j.organization.organizationName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            ORDER BY j.createdOn DESC
            """)
    Page<ProvisioningJob> searchJobs(
            @Param("status") ProvisionJobStatus status,
            @Param("search") String search,
            Pageable pageable);
}
