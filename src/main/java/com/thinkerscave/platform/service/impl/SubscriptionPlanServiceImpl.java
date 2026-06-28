package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.SubscriptionPlanFeatureRequest;
import com.thinkerscave.platform.dto.request.SubscriptionPlanRequest;
import com.thinkerscave.platform.dto.response.SubscriptionPlanFeatureResponse;
import com.thinkerscave.platform.dto.response.SubscriptionPlanResponse;
import com.thinkerscave.platform.entity.Feature;
import com.thinkerscave.platform.entity.SubscriptionPlan;
import com.thinkerscave.platform.entity.SubscriptionPlanFeature;
import com.thinkerscave.platform.repository.FeatureRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanFeatureRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanRepository;
import com.thinkerscave.platform.service.SubscriptionPlanService;
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
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPlanFeatureRepository planFeatureRepository;
    private final FeatureRepository featureRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAllPlans() {
        return planRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream().map(this::toPlanResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanById(Long id) {
        return toPlanResponse(findPlanById(id));
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse createPlan(SubscriptionPlanRequest request) {
        if (planRepository.existsByPlanCode(request.getPlanCode())) {
            throw new AlreadyExistsException("Plan code already exists: " + request.getPlanCode());
        }
        if (planRepository.existsByPlanName(request.getPlanName())) {
            throw new AlreadyExistsException("Plan name already exists: " + request.getPlanName());
        }
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planCode(request.getPlanCode())
                .planName(request.getPlanName())
                .description(request.getDescription())
                .monthlyPrice(request.getMonthlyPrice())
                .quarterlyPrice(request.getQuarterlyPrice())
                .halfYearlyPrice(request.getHalfYearlyPrice())
                .yearlyPrice(request.getYearlyPrice())
                .studentLimit(request.getStudentLimit())
                .staffLimit(request.getStaffLimit())
                .branchLimit(request.getBranchLimit())
                .storageLimitGb(request.getStorageLimitGb())
                .apiRequestLimit(request.getApiRequestLimit())
                .trialDays(request.getTrialDays() != null ? request.getTrialDays() : 0)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .recommended(Boolean.TRUE.equals(request.getRecommended()))
                .customPlan(Boolean.TRUE.equals(request.getCustomPlan()))
                .visible(request.getVisible() == null || request.getVisible())
                .active(true)
                .remarks(request.getRemarks())
                .build();
        return toPlanResponse(planRepository.save(plan));
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse updatePlan(Long id, SubscriptionPlanRequest request) {
        SubscriptionPlan plan = findPlanById(id);
        if (planRepository.existsByPlanCodeAndIdNot(request.getPlanCode(), id)) {
            throw new AlreadyExistsException("Plan code already exists: " + request.getPlanCode());
        }
        if (planRepository.existsByPlanNameAndIdNot(request.getPlanName(), id)) {
            throw new AlreadyExistsException("Plan name already exists: " + request.getPlanName());
        }
        plan.setPlanCode(request.getPlanCode());
        plan.setPlanName(request.getPlanName());
        plan.setDescription(request.getDescription());
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setQuarterlyPrice(request.getQuarterlyPrice());
        plan.setHalfYearlyPrice(request.getHalfYearlyPrice());
        plan.setYearlyPrice(request.getYearlyPrice());
        plan.setStudentLimit(request.getStudentLimit());
        plan.setStaffLimit(request.getStaffLimit());
        plan.setBranchLimit(request.getBranchLimit());
        plan.setStorageLimitGb(request.getStorageLimitGb());
        plan.setApiRequestLimit(request.getApiRequestLimit());
        if (request.getTrialDays() != null) plan.setTrialDays(request.getTrialDays());
        if (request.getDisplayOrder() != null) plan.setDisplayOrder(request.getDisplayOrder());
        if (request.getRecommended() != null) plan.setRecommended(request.getRecommended());
        if (request.getCustomPlan() != null) plan.setCustomPlan(request.getCustomPlan());
        if (request.getVisible() != null) plan.setVisible(request.getVisible());
        plan.setRemarks(request.getRemarks());
        return toPlanResponse(planRepository.save(plan));
    }

    @Override
    @Transactional
    public void archivePlan(Long id) {
        SubscriptionPlan plan = findPlanById(id);
        plan.setActive(false);
        plan.setVisible(false);
        planRepository.save(plan);
        log.info("Subscription plan archived: {}", plan.getPlanCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanFeatureResponse> getPlanFeatures(Long planId) {
        findPlanById(planId);
        return planFeatureRepository.findBySubscriptionPlan_IdAndActiveTrueOrderByDisplayOrderAsc(planId)
                .stream().map(this::toPlanFeatureResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubscriptionPlanFeatureResponse addPlanFeature(SubscriptionPlanFeatureRequest request) {
        SubscriptionPlan plan = findPlanById(request.getSubscriptionPlanId());
        Feature feature = featureRepository.findById(request.getFeatureId())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + request.getFeatureId()));
        if (planFeatureRepository.existsBySubscriptionPlan_IdAndFeature_Id(plan.getId(), feature.getId())) {
            throw new AlreadyExistsException("Feature already added to this plan");
        }
        SubscriptionPlanFeature spf = SubscriptionPlanFeature.builder()
                .subscriptionPlan(plan)
                .feature(feature)
                .enabled(request.getEnabled() == null || request.getEnabled())
                .mandatory(Boolean.TRUE.equals(request.getMandatory()))
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .notes(request.getNotes())
                .active(true)
                .build();
        return toPlanFeatureResponse(planFeatureRepository.save(spf));
    }

    @Override
    @Transactional
    public SubscriptionPlanFeatureResponse updatePlanFeature(Long id, SubscriptionPlanFeatureRequest request) {
        SubscriptionPlanFeature spf = planFeatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlanFeature not found: " + id));
        if (request.getEnabled() != null) spf.setEnabled(request.getEnabled());
        if (request.getMandatory() != null) spf.setMandatory(request.getMandatory());
        if (request.getDisplayOrder() != null) spf.setDisplayOrder(request.getDisplayOrder());
        spf.setNotes(request.getNotes());
        return toPlanFeatureResponse(planFeatureRepository.save(spf));
    }

    @Override
    @Transactional
    public void removePlanFeature(Long id) {
        SubscriptionPlanFeature spf = planFeatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlanFeature not found: " + id));
        spf.setActive(false);
        planFeatureRepository.save(spf);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SubscriptionPlan findPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan not found: " + id));
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlan p) {
        List<SubscriptionPlanFeatureResponse> features =
                planFeatureRepository.findBySubscriptionPlan_IdAndActiveTrueOrderByDisplayOrderAsc(p.getId())
                        .stream().map(this::toPlanFeatureResponse).collect(Collectors.toList());
        return SubscriptionPlanResponse.builder()
                .id(p.getId())
                .planCode(p.getPlanCode())
                .planName(p.getPlanName())
                .description(p.getDescription())
                .monthlyPrice(p.getMonthlyPrice())
                .quarterlyPrice(p.getQuarterlyPrice())
                .halfYearlyPrice(p.getHalfYearlyPrice())
                .yearlyPrice(p.getYearlyPrice())
                .studentLimit(p.getStudentLimit())
                .staffLimit(p.getStaffLimit())
                .branchLimit(p.getBranchLimit())
                .storageLimitGb(p.getStorageLimitGb())
                .apiRequestLimit(p.getApiRequestLimit())
                .trialDays(p.getTrialDays())
                .displayOrder(p.getDisplayOrder())
                .recommended(p.getRecommended())
                .customPlan(p.getCustomPlan())
                .visible(p.getVisible())
                .active(p.getActive())
                .remarks(p.getRemarks())
                .features(features)
                .createdOn(p.getCreatedOn())
                .createdBy(p.getCreatedBy())
                .updatedOn(p.getUpdatedOn())
                .updatedBy(p.getUpdatedBy())
                .build();
    }

    private SubscriptionPlanFeatureResponse toPlanFeatureResponse(SubscriptionPlanFeature spf) {
        return SubscriptionPlanFeatureResponse.builder()
                .id(spf.getId())
                .subscriptionPlanId(spf.getSubscriptionPlan().getId())
                .planName(spf.getSubscriptionPlan().getPlanName())
                .featureId(spf.getFeature().getId())
                .featureCode(spf.getFeature().getFeatureCode())
                .featureName(spf.getFeature().getFeatureName())
                .featureKey(spf.getFeature().getFeatureKey())
                .module(spf.getFeature().getModule())
                .enabled(spf.getEnabled())
                .mandatory(spf.getMandatory())
                .displayOrder(spf.getDisplayOrder())
                .notes(spf.getNotes())
                .active(spf.getActive())
                .createdOn(spf.getCreatedOn())
                .build();
    }
}
