package com.thinkerscave.common.exam.repository;

import com.thinkerscave.common.exam.domain.Result;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long>, JpaSpecificationExecutor<Result> {

    Optional<Result> findByExamIdAndStudentId(Long examId, Long studentId);

    Page<Result> findByExamId(Long examId, Pageable pageable);

    List<Result> findByOrganizationIdAndStudentIdOrderByIdDesc(Long organizationId, Long studentId);
}
