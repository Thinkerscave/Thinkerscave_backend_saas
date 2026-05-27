package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeStructure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeStructureRepository
        extends JpaRepository<FeeStructure, Long>, JpaSpecificationExecutor<FeeStructure> {

    Page<FeeStructure> findByOrganizationId(Long organizationId, Pageable pageable);

    List<FeeStructure> findByOrganizationIdAndAcademicYearId(Long organizationId, Long academicYearId);

    List<FeeStructure> findByOrganizationIdAndAcademicYearIdAndClassId(
            Long organizationId, Long academicYearId, Long classId);
}
