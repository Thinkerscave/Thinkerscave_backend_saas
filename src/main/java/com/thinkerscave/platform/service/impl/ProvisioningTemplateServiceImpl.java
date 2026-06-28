package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.ProvisioningTemplateItemRequest;
import com.thinkerscave.platform.dto.request.ProvisioningTemplateRequest;
import com.thinkerscave.platform.dto.response.ProvisioningTemplateItemResponse;
import com.thinkerscave.platform.dto.response.ProvisioningTemplateResponse;
import com.thinkerscave.platform.entity.ProvisioningTemplate;
import com.thinkerscave.platform.entity.ProvisioningTemplateItem;
import com.thinkerscave.platform.repository.ProvisioningTemplateItemRepository;
import com.thinkerscave.platform.repository.ProvisioningTemplateRepository;
import com.thinkerscave.platform.service.ProvisioningTemplateService;
import com.thinkerscave.shared.enums.CodeType;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisioningTemplateServiceImpl implements ProvisioningTemplateService {

    private final ProvisioningTemplateRepository templateRepository;
    private final ProvisioningTemplateItemRepository itemRepository;
    private final CodeGeneratorService codeGeneratorService;

    @Override
    @Transactional(readOnly = true)
    public List<ProvisioningTemplateResponse> getAllTemplates() {
        return templateRepository.findByActiveTrueOrderByInstitutionTypeAscTemplateNameAsc()
                .stream().map(t -> toTemplateResponse(t, false)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisioningTemplateResponse getTemplateById(Long id) {
        return toTemplateResponse(findById(id), true);
    }

    @Override
    @Transactional
    public ProvisioningTemplateResponse createTemplate(ProvisioningTemplateRequest request) {
        if (templateRepository.existsByTemplateCode(request.getTemplateCode())) {
            throw new AlreadyExistsException("Template code already exists: " + request.getTemplateCode());
        }
        if (templateRepository.existsByTemplateName(request.getTemplateName())) {
            throw new AlreadyExistsException("Template name already exists: " + request.getTemplateName());
        }
        ProvisioningTemplate template = ProvisioningTemplate.builder()
                .templateCode(request.getTemplateCode())
                .templateName(request.getTemplateName())
                .institutionType(request.getInstitutionType())
                .templateVersion(request.getTemplateVersion())
                .description(request.getDescription())
                .academicStructureEnabled(request.getAcademicStructureEnabled() == null || request.getAcademicStructureEnabled())
                .rolesEnabled(request.getRolesEnabled() == null || request.getRolesEnabled())
                .permissionsEnabled(request.getPermissionsEnabled() == null || request.getPermissionsEnabled())
                .classesEnabled(request.getClassesEnabled() == null || request.getClassesEnabled())
                .sectionsEnabled(request.getSectionsEnabled() == null || request.getSectionsEnabled())
                .departmentsEnabled(request.getDepartmentsEnabled() == null || request.getDepartmentsEnabled())
                .designationsEnabled(request.getDesignationsEnabled() == null || request.getDesignationsEnabled())
                .seedMasterData(request.getSeedMasterData() == null || request.getSeedMasterData())
                .active(true)
                .remarks(request.getRemarks())
                .build();
        return toTemplateResponse(templateRepository.save(template), false);
    }

    @Override
    @Transactional
    public ProvisioningTemplateResponse updateTemplate(Long id, ProvisioningTemplateRequest request) {
        ProvisioningTemplate template = findById(id);
        if (templateRepository.existsByTemplateCodeAndIdNot(request.getTemplateCode(), id)) {
            throw new AlreadyExistsException("Template code already exists: " + request.getTemplateCode());
        }
        if (templateRepository.existsByTemplateNameAndIdNot(request.getTemplateName(), id)) {
            throw new AlreadyExistsException("Template name already exists: " + request.getTemplateName());
        }
        template.setTemplateCode(request.getTemplateCode());
        template.setTemplateName(request.getTemplateName());
        template.setInstitutionType(request.getInstitutionType());
        template.setTemplateVersion(request.getTemplateVersion());
        template.setDescription(request.getDescription());
        if (request.getAcademicStructureEnabled() != null) template.setAcademicStructureEnabled(request.getAcademicStructureEnabled());
        if (request.getRolesEnabled() != null) template.setRolesEnabled(request.getRolesEnabled());
        if (request.getPermissionsEnabled() != null) template.setPermissionsEnabled(request.getPermissionsEnabled());
        if (request.getClassesEnabled() != null) template.setClassesEnabled(request.getClassesEnabled());
        if (request.getSectionsEnabled() != null) template.setSectionsEnabled(request.getSectionsEnabled());
        if (request.getDepartmentsEnabled() != null) template.setDepartmentsEnabled(request.getDepartmentsEnabled());
        if (request.getDesignationsEnabled() != null) template.setDesignationsEnabled(request.getDesignationsEnabled());
        if (request.getSeedMasterData() != null) template.setSeedMasterData(request.getSeedMasterData());
        template.setRemarks(request.getRemarks());
        return toTemplateResponse(templateRepository.save(template), false);
    }

    @Override
    @Transactional
    public void archiveTemplate(Long id) {
        ProvisioningTemplate template = findById(id);
        template.setActive(false);
        templateRepository.save(template);
        log.info("Provisioning template archived: {}", template.getTemplateCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProvisioningTemplateItemResponse> getTemplateItems(Long templateId) {
        findById(templateId);
        return itemRepository.findByTemplate_IdAndActiveTrueOrderByDisplayOrderAsc(templateId)
                .stream().map(this::toItemResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProvisioningTemplateItemResponse addTemplateItem(ProvisioningTemplateItemRequest request) {
        ProvisioningTemplate template = findById(request.getTemplateId());
        if (itemRepository.existsByTemplate_IdAndItemKey(template.getId(), request.getItemKey())) {
            throw new AlreadyExistsException("Item key already exists in this template: " + request.getItemKey());
        }
        ProvisioningTemplateItem item = ProvisioningTemplateItem.builder()
                .template(template)
                .itemType(request.getItemType())
                .itemKey(request.getItemKey())
                .itemName(request.getItemName())
                .itemValue(request.getItemValue())
                .configurationJson(request.getConfigurationJson())
                .mandatory(Boolean.TRUE.equals(request.getMandatory()))
                .enabled(request.getEnabled() == null || request.getEnabled())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(true)
                .remarks(request.getRemarks())
                .build();
        return toItemResponse(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ProvisioningTemplateItemResponse updateTemplateItem(Long id, ProvisioningTemplateItemRequest request) {
        ProvisioningTemplateItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProvisioningTemplateItem not found: " + id));
        if (itemRepository.existsByTemplate_IdAndItemKeyAndIdNot(item.getTemplate().getId(), request.getItemKey(), id)) {
            throw new AlreadyExistsException("Item key already exists in this template: " + request.getItemKey());
        }
        item.setItemType(request.getItemType());
        item.setItemKey(request.getItemKey());
        item.setItemName(request.getItemName());
        item.setItemValue(request.getItemValue());
        item.setConfigurationJson(request.getConfigurationJson());
        if (request.getMandatory() != null) item.setMandatory(request.getMandatory());
        if (request.getEnabled() != null) item.setEnabled(request.getEnabled());
        if (request.getDisplayOrder() != null) item.setDisplayOrder(request.getDisplayOrder());
        item.setRemarks(request.getRemarks());
        return toItemResponse(itemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteTemplateItem(Long id) {
        ProvisioningTemplateItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProvisioningTemplateItem not found: " + id));
        item.setActive(false);
        itemRepository.save(item);
    }

    private ProvisioningTemplate findById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProvisioningTemplate not found: " + id));
    }

    private ProvisioningTemplateResponse toTemplateResponse(ProvisioningTemplate t, boolean withItems) {
        List<ProvisioningTemplateItemResponse> items = null;
        if (withItems) {
            items = itemRepository.findByTemplate_IdAndActiveTrueOrderByDisplayOrderAsc(t.getId())
                    .stream().map(this::toItemResponse).collect(Collectors.toList());
        }
        return ProvisioningTemplateResponse.builder()
                .id(t.getId())
                .templateCode(t.getTemplateCode())
                .templateName(t.getTemplateName())
                .institutionType(t.getInstitutionType())
                .templateVersion(t.getTemplateVersion())
                .description(t.getDescription())
                .academicStructureEnabled(t.getAcademicStructureEnabled())
                .rolesEnabled(t.getRolesEnabled())
                .permissionsEnabled(t.getPermissionsEnabled())
                .classesEnabled(t.getClassesEnabled())
                .sectionsEnabled(t.getSectionsEnabled())
                .departmentsEnabled(t.getDepartmentsEnabled())
                .designationsEnabled(t.getDesignationsEnabled())
                .seedMasterData(t.getSeedMasterData())
                .active(t.getActive())
                .remarks(t.getRemarks())
                .items(items)
                .createdOn(t.getCreatedOn())
                .createdBy(t.getCreatedBy())
                .updatedOn(t.getUpdatedOn())
                .updatedBy(t.getUpdatedBy())
                .build();
    }

    private ProvisioningTemplateItemResponse toItemResponse(ProvisioningTemplateItem i) {
        return ProvisioningTemplateItemResponse.builder()
                .id(i.getId())
                .templateId(i.getTemplate().getId())
                .itemType(i.getItemType())
                .itemKey(i.getItemKey())
                .itemName(i.getItemName())
                .itemValue(i.getItemValue())
                .configurationJson(i.getConfigurationJson())
                .mandatory(i.getMandatory())
                .enabled(i.getEnabled())
                .displayOrder(i.getDisplayOrder())
                .active(i.getActive())
                .remarks(i.getRemarks())
                .createdOn(i.getCreatedOn())
                .build();
    }
}
