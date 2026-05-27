package com.thinkerscave.common.exam.repository;

import com.thinkerscave.common.exam.domain.ExamType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamTypeRepository extends JpaRepository<ExamType, Long> {
    List<ExamType> findByOrganizationId(Long organizationId);
    Optional<ExamType> findByOrganizationIdAndCode(Long organizationId, String code);
}
