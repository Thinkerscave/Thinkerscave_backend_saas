package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.FeatureRequest;
import com.thinkerscave.platform.dto.response.FeatureResponse;
import com.thinkerscave.platform.entity.Feature;
import com.thinkerscave.platform.repository.FeatureRepository;
import com.thinkerscave.platform.service.FeatureService;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureServiceImpl implements FeatureService {

    private final FeatureRepository featureRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FeatureResponse> getAllFeatures() {
        return featureRepository.findByActiveTrueOrderByModuleAscDisplayOrderAsc()
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
        if (featureRepository.existsByFeatureCode(request.getFeatureCode())) {
            throw new AlreadyExistsException("Feature code already exists: " + request.getFeatureCode());
        }
        if (featureRepository.existsByFeatureKey(request.getFeatureKey())) {
            throw new AlreadyExistsException("Feature key already exists: " + request.getFeatureKey());
        }
        Feature feature = Feature.builder()
                .featureCode(request.getFeatureCode())
                .featureName(request.getFeatureName())
                .displayName(request.getDisplayName())
                .module(request.getModule())
                .category(request.getCategory())
                .featureKey(request.getFeatureKey())
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
        if (featureRepository.existsByFeatureCodeAndIdNot(request.getFeatureCode(), id)) {
            throw new AlreadyExistsException("Feature code already exists: " + request.getFeatureCode());
        }
        if (featureRepository.existsByFeatureKeyAndIdNot(request.getFeatureKey(), id)) {
            throw new AlreadyExistsException("Feature key already exists: " + request.getFeatureKey());
        }
        feature.setFeatureCode(request.getFeatureCode());
        feature.setFeatureName(request.getFeatureName());
        feature.setDisplayName(request.getDisplayName());
        feature.setModule(request.getModule());
        feature.setCategory(request.getCategory());
        feature.setFeatureKey(request.getFeatureKey());
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
