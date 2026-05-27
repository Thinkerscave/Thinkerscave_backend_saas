package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeContractRepository
        extends JpaRepository<FeeContract, Long>, JpaSpecificationExecutor<FeeContract> {

    Optional<FeeContract> findByEnrollmentId(Long enrollmentId);

    List<FeeContract> findByOrganizationIdAndStudentId(Long organizationId, Long studentId);

    Page<FeeContract> findByOrganizationIdAndAcademicYearId(
            Long organizationId, Long academicYearId, Pageable pageable);
}
