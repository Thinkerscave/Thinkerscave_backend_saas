package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.SubscriptionPlanFeatureRequest;
import com.thinkerscave.platform.dto.request.SubscriptionPlanRequest;
import com.thinkerscave.platform.dto.response.SubscriptionPlanFeatureResponse;
import com.thinkerscave.platform.dto.response.SubscriptionPlanResponse;

import java.util.List;

public interface SubscriptionPlanService {

    List<SubscriptionPlanResponse> getAllPlans();

    /** Active plans marked visible — for unauthenticated marketing/pricing surfaces. */
    List<SubscriptionPlanResponse> getVisiblePublicPlans();

    SubscriptionPlanResponse getPlanById(Long id);

    SubscriptionPlanResponse createPlan(SubscriptionPlanRequest request);

    SubscriptionPlanResponse updatePlan(Long id, SubscriptionPlanRequest request);

    void archivePlan(Long id);

    // Plan Features
    List<SubscriptionPlanFeatureResponse> getPlanFeatures(Long planId);

    SubscriptionPlanFeatureResponse addPlanFeature(SubscriptionPlanFeatureRequest request);

    SubscriptionPlanFeatureResponse updatePlanFeature(Long id, SubscriptionPlanFeatureRequest request);

    void removePlanFeature(Long id);
}
