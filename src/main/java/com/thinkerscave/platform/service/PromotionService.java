package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.PromotionRequest;
import com.thinkerscave.platform.dto.response.PromotionResponse;

import java.util.List;

public interface PromotionService {

    List<PromotionResponse> getAllPromotions();

    PromotionResponse getPromotionById(Long id);

    PromotionResponse createPromotion(PromotionRequest request);

    PromotionResponse updatePromotion(Long id, PromotionRequest request);

    void archivePromotion(Long id);
}
