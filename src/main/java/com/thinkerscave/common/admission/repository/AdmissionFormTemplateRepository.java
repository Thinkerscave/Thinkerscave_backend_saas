package com.thinkerscave.common.admission.repository;

import com.thinkerscave.common.admission.domain.AdmissionFormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdmissionFormTemplateRepository extends JpaRepository<AdmissionFormTemplate, Long> {
    Optional<AdmissionFormTemplate> findByTenantIdAndIsActiveTrue(String tenantId);
}
