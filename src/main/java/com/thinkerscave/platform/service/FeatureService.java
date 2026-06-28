package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.FeatureRequest;
import com.thinkerscave.platform.dto.response.FeatureResponse;

import java.util.List;

public interface FeatureService {

    List<FeatureResponse> getAllFeatures();

    FeatureResponse getFeatureById(Long id);

    FeatureResponse createFeature(FeatureRequest request);

    FeatureResponse updateFeature(Long id, FeatureRequest request);

    void deleteFeature(Long id);
}
