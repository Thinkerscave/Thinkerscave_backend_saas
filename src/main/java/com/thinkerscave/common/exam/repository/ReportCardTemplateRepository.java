package com.thinkerscave.common.exam.repository;

import com.thinkerscave.common.exam.domain.ReportCardTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportCardTemplateRepository extends JpaRepository<ReportCardTemplate, Long> {
    List<ReportCardTemplate> findByOrganizationId(Long organizationId);
    Optional<ReportCardTemplate> findByOrganizationIdAndCode(Long organizationId, String code);
}
