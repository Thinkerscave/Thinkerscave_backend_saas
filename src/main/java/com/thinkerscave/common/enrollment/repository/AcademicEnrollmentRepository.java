package com.thinkerscave.common.enrollment.repository;

import com.thinkerscave.common.enrollment.domain.AcademicEnrollment;
import com.thinkerscave.common.enrollment.domain.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicEnrollmentRepository
        extends JpaRepository<AcademicEnrollment, Long>, JpaSpecificationExecutor<AcademicEnrollment> {

    Optional<AcademicEnrollment> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    Page<AcademicEnrollment> findByOrganizationIdAndAcademicYearId(
            Long organizationId, Long academicYearId, Pageable pageable);

    List<AcademicEnrollment> findByOrganizationIdAndAcademicYearIdAndClassIdAndStatus(
            Long organizationId, Long academicYearId, Long classId, EnrollmentStatus status);

    long countByOrganizationIdAndAcademicYearIdAndStatus(
            Long organizationId, Long academicYearId, EnrollmentStatus status);

    boolean existsByEnrollmentNumberAndOrganizationId(String enrollmentNumber, Long organizationId);
}
