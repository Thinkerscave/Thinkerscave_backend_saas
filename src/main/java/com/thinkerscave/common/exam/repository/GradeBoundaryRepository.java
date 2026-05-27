package com.thinkerscave.common.exam.repository;

import com.thinkerscave.common.exam.domain.GradeBoundary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeBoundaryRepository extends JpaRepository<GradeBoundary, Long> {
    List<GradeBoundary> findByGradingScaleIdOrderByDisplayOrderAsc(Long gradingScaleId);
    void deleteByGradingScaleId(Long gradingScaleId);
}
