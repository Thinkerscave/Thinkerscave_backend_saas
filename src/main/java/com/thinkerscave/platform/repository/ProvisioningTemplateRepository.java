package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.ProvisioningTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProvisioningTemplateRepository extends JpaRepository<ProvisioningTemplate, Long> {

    boolean existsByTemplateCode(String templateCode);

    boolean existsByTemplateName(String templateName);

    boolean existsByTemplateCodeAndIdNot(String templateCode, Long id);

    boolean existsByTemplateNameAndIdNot(String templateName, Long id);

    Optional<ProvisioningTemplate> findByTemplateCode(String templateCode);

    List<ProvisioningTemplate> findByActiveTrueOrderByInstitutionTypeAscTemplateNameAsc();

    List<ProvisioningTemplate> findByInstitutionTypeAndActiveTrue(String institutionType);
}
