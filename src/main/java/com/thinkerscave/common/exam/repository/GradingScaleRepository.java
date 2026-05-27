package com.thinkerscave.common.exam.repository;

import com.thinkerscave.common.exam.domain.GradingScale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradingScaleRepository extends JpaRepository<GradingScale, Long> {
    List<GradingScale> findByOrganizationId(Long organizationId);
    Optional<GradingScale> findByOrganizationIdAndCode(Long organizationId, String code);
}
