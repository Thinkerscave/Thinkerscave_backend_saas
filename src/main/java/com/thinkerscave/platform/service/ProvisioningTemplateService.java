package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.ProvisioningTemplateItemRequest;
import com.thinkerscave.platform.dto.request.ProvisioningTemplateRequest;
import com.thinkerscave.platform.dto.response.ProvisioningTemplateItemResponse;
import com.thinkerscave.platform.dto.response.ProvisioningTemplateResponse;

import java.util.List;

public interface ProvisioningTemplateService {

    List<ProvisioningTemplateResponse> getAllTemplates();

    ProvisioningTemplateResponse getTemplateById(Long id);

    ProvisioningTemplateResponse createTemplate(ProvisioningTemplateRequest request);

    ProvisioningTemplateResponse updateTemplate(Long id, ProvisioningTemplateRequest request);

    void archiveTemplate(Long id);

    // Items
    List<ProvisioningTemplateItemResponse> getTemplateItems(Long templateId);

    ProvisioningTemplateItemResponse addTemplateItem(ProvisioningTemplateItemRequest request);

    ProvisioningTemplateItemResponse updateTemplateItem(Long id, ProvisioningTemplateItemRequest request);

    void deleteTemplateItem(Long id);
}
