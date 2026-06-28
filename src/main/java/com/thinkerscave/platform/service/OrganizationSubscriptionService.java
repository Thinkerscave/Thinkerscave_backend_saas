package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.FeatureOverrideRequest;
import com.thinkerscave.platform.dto.request.OrganizationPromotionRequest;
import com.thinkerscave.platform.dto.request.OrganizationSubscriptionRequest;
import com.thinkerscave.platform.dto.request.SubscriptionChangeRequest;
import com.thinkerscave.platform.dto.response.FeatureOverrideResponse;
import com.thinkerscave.platform.dto.response.OrganizationPromotionResponse;
import com.thinkerscave.platform.dto.response.OrganizationSubscriptionResponse;
import com.thinkerscave.platform.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrganizationSubscriptionService {

    Page<OrganizationSubscriptionResponse> getSubscriptions(SubscriptionStatus status, String search, Pageable pageable);

    OrganizationSubscriptionResponse getSubscriptionById(Long id);

    OrganizationSubscriptionResponse createSubscription(OrganizationSubscriptionRequest request);

    OrganizationSubscriptionResponse updateSubscription(Long id, OrganizationSubscriptionRequest request);

    OrganizationSubscriptionResponse renewSubscription(Long id, SubscriptionChangeRequest request);

    OrganizationSubscriptionResponse upgradeSubscription(Long id, SubscriptionChangeRequest request);

    OrganizationSubscriptionResponse downgradeSubscription(Long id, SubscriptionChangeRequest request);

    void cancelSubscription(Long id);

    // Organization Promotions
    OrganizationPromotionResponse applyPromotion(OrganizationPromotionRequest request);

    void removePromotion(Long id);

    // Feature Overrides
    List<FeatureOverrideResponse> getFeatureOverrides(Long subscriptionId);

    FeatureOverrideResponse createFeatureOverride(FeatureOverrideRequest request);

    FeatureOverrideResponse updateFeatureOverride(Long id, FeatureOverrideRequest request);

    void deleteFeatureOverride(Long id);
}
