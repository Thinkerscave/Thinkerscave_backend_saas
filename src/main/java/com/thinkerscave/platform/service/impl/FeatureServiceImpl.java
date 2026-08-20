package com.thinkerscave.platform.service.impl;

import com.thinkerscave.access.dto.response.MenuResponse;
import com.thinkerscave.access.entity.Menu;
import com.thinkerscave.access.enums.MenuScope;
import com.thinkerscave.access.mapper.MenuMapper;
import com.thinkerscave.access.repository.MenuRepository;
import com.thinkerscave.platform.dto.request.FeatureMenuMappingRequest;
import com.thinkerscave.platform.dto.request.FeatureRequest;
import com.thinkerscave.platform.dto.response.FeatureResponse;
import com.thinkerscave.platform.entity.Feature;
import com.thinkerscave.platform.repository.FeatureRepository;
import com.thinkerscave.platform.service.FeatureService;
import com.thinkerscave.platform.service.TenantCatalogSyncService;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureServiceImpl implements FeatureService {

    private final FeatureRepository featureRepository;
    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final TenantCatalogSyncService tenantCatalogSyncService;

    @Override
    @Transactional(readOnly = true)
    public List<FeatureResponse> getAllFeatures() {
        return featureRepository.findAllByOrderByModuleAscDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FeatureResponse getFeatureById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public FeatureResponse createFeature(FeatureRequest request) {
        String featureKey = StringUtils.hasText(request.getFeatureKey())
                ? request.getFeatureKey()
                : request.getFeatureCode();
        if (featureRepository.existsByFeatureCode(request.getFeatureCode())) {
            throw new AlreadyExistsException("Feature code already exists: " + request.getFeatureCode());
        }
        if (featureRepository.existsByFeatureKey(featureKey)) {
            throw new AlreadyExistsException("Feature key already exists: " + featureKey);
        }
        Feature feature = Feature.builder()
                .featureCode(request.getFeatureCode())
                .featureName(request.getFeatureName())
                .displayName(request.getDisplayName())
                .module(request.getModule())
                .category(request.getCategory())
                .featureKey(featureKey)
                .description(request.getDescription())
                .icon(request.getIcon())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .premiumFeature(request.getPremiumFeature() != null && request.getPremiumFeature())
                .visible(request.getVisible() == null || request.getVisible())
                .defaultEnabled(request.getDefaultEnabled() != null && request.getDefaultEnabled())
                .remarks(request.getRemarks())
                .active(true)
                .build();
        if (request.getParentFeatureId() != null) {
            feature.setParentFeature(findById(request.getParentFeatureId()));
        }
        return toResponse(featureRepository.save(feature));
    }

    @Override
    @Transactional
    public FeatureResponse updateFeature(Long id, FeatureRequest request) {
        Feature feature = findById(id);
        String featureKey = StringUtils.hasText(request.getFeatureKey())
                ? request.getFeatureKey()
                : request.getFeatureCode();
        if (featureRepository.existsByFeatureCodeAndIdNot(request.getFeatureCode(), id)) {
            throw new AlreadyExistsException("Feature code already exists: " + request.getFeatureCode());
        }
        if (featureRepository.existsByFeatureKeyAndIdNot(featureKey, id)) {
            throw new AlreadyExistsException("Feature key already exists: " + featureKey);
        }
        feature.setFeatureCode(request.getFeatureCode());
        feature.setFeatureName(request.getFeatureName());
        feature.setDisplayName(request.getDisplayName());
        feature.setModule(request.getModule());
        feature.setCategory(request.getCategory());
        feature.setFeatureKey(featureKey);
        feature.setDescription(request.getDescription());
        feature.setIcon(request.getIcon());
        if (request.getDisplayOrder() != null) feature.setDisplayOrder(request.getDisplayOrder());
        if (request.getPremiumFeature() != null) feature.setPremiumFeature(request.getPremiumFeature());
        if (request.getVisible() != null) feature.setVisible(request.getVisible());
        if (request.getDefaultEnabled() != null) feature.setDefaultEnabled(request.getDefaultEnabled());
        feature.setRemarks(request.getRemarks());
        if (request.getParentFeatureId() != null) {
            feature.setParentFeature(findById(request.getParentFeatureId()));
        } else {
            feature.setParentFeature(null);
        }
        return toResponse(featureRepository.save(feature));
    }

    @Override
    @Transactional
    public void deleteFeature(Long id) {
        Feature feature = findById(id);
        feature.setActive(false);
        featureRepository.save(feature);
        log.info("Feature archived: {}", feature.getFeatureCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getFeatureMenus(Long featureId) {
        findById(featureId);
        return menuRepository.findByFeature_Id(featureId).stream()
                .map(menuMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<MenuResponse> replaceFeatureMenus(Long featureId, FeatureMenuMappingRequest request) {
        Feature feature = findById(featureId);
        Set<Long> desired = new HashSet<>(request.getMenuIds() == null ? List.of() : request.getMenuIds());
        List<Menu> currentlyMapped = menuRepository.findByFeature_Id(featureId);
        for (Menu menu : currentlyMapped) {
            if (!desired.contains(menu.getId())) {
                menu.setFeature(null);
                menuRepository.save(menu);
                if (Boolean.TRUE.equals(menu.getActive())) {
                    tenantCatalogSyncService.syncMenu(menu);
                }
            }
        }
        for (Long menuId : desired) {
            Menu menu = menuRepository.findById(menuId)
                    .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + menuId));
            if (menu.getParentMenu() != null) {
                throw new BadRequestException("Only top-level menus can be mapped to a feature: " + menu.getMenuCode());
            }
            if (menu.getMenuScope() == MenuScope.PLATFORM) {
                throw new BadRequestException("Platform menus cannot be mapped to a subscription feature");
            }
            menu.setFeature(feature);
            menu.setMenuScope(MenuScope.SUBSCRIPTION);
            menuRepository.save(menu);
            if (Boolean.TRUE.equals(menu.getActive())) {
                tenantCatalogSyncService.syncMenu(menu);
            }
        }
        return getFeatureMenus(featureId);
    }

    private Feature findById(Long id) {
        return featureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + id));
    }

    private FeatureResponse toResponse(Feature f) {
        return FeatureResponse.builder()
                .id(f.getId())
                .featureCode(f.getFeatureCode())
                .featureName(f.getFeatureName())
                .displayName(f.getDisplayName())
                .module(f.getModule())
                .category(f.getCategory())
                .parentFeatureId(f.getParentFeature() != null ? f.getParentFeature().getId() : null)
                .parentFeatureName(f.getParentFeature() != null ? f.getParentFeature().getFeatureName() : null)
                .featureKey(f.getFeatureKey())
                .description(f.getDescription())
                .icon(f.getIcon())
                .displayOrder(f.getDisplayOrder())
                .premiumFeature(f.getPremiumFeature())
                .visible(f.getVisible())
                .defaultEnabled(f.getDefaultEnabled())
                .active(f.getActive())
                .remarks(f.getRemarks())
                .createdOn(f.getCreatedOn())
                .createdBy(f.getCreatedBy())
                .updatedOn(f.getUpdatedOn())
                .updatedBy(f.getUpdatedBy())
                .build();
    }
}
