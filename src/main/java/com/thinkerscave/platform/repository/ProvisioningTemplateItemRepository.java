package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.ProvisioningTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvisioningTemplateItemRepository extends JpaRepository<ProvisioningTemplateItem, Long> {

    List<ProvisioningTemplateItem> findByTemplate_IdAndActiveTrueOrderByDisplayOrderAsc(Long templateId);

    boolean existsByTemplate_IdAndItemKey(Long templateId, String itemKey);

    boolean existsByTemplate_IdAndItemKeyAndIdNot(Long templateId, String itemKey, Long id);

    void deleteByTemplate_Id(Long templateId);
}
