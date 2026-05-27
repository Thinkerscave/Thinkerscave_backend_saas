package com.thinkerscave.common.exam.repository;

import com.thinkerscave.common.exam.domain.Exam;
import com.thinkerscave.common.exam.domain.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long>, JpaSpecificationExecutor<Exam> {

    Optional<Exam> findByOrganizationIdAndCode(Long organizationId, String code);

    Page<Exam> findByOrganizationIdAndAcademicYearId(
            Long organizationId, Long academicYearId, Pageable pageable);

    List<Exam> findByOrganizationIdAndAcademicYearIdAndClassIdAndStatus(
            Long organizationId, Long academicYearId, Long classId, ExamStatus status);
}
