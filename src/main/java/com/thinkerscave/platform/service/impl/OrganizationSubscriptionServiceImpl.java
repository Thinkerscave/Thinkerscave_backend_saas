package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.FeatureOverrideRequest;
import com.thinkerscave.platform.dto.request.OrganizationPromotionRequest;
import com.thinkerscave.platform.dto.request.OrganizationSubscriptionRequest;
import com.thinkerscave.platform.dto.request.SubscriptionChangeRequest;
import com.thinkerscave.platform.dto.response.FeatureOverrideResponse;
import com.thinkerscave.platform.dto.response.OrganizationPromotionResponse;
import com.thinkerscave.platform.dto.response.OrganizationSubscriptionResponse;
import com.thinkerscave.platform.entity.Feature;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.entity.OrganizationPromotion;
import com.thinkerscave.platform.entity.OrganizationSubscription;
import com.thinkerscave.platform.entity.Promotion;
import com.thinkerscave.platform.entity.SubscriptionFeatureOverride;
import com.thinkerscave.platform.entity.SubscriptionPlan;
import com.thinkerscave.platform.enums.SubscriptionStatus;
import com.thinkerscave.platform.repository.FeatureRepository;
import com.thinkerscave.platform.repository.OrganizationPromotionRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.OrganizationSubscriptionRepository;
import com.thinkerscave.platform.repository.PromotionRepository;
import com.thinkerscave.platform.repository.SubscriptionFeatureOverrideRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanRepository;
import com.thinkerscave.platform.service.OrganizationSubscriptionService;
import com.thinkerscave.platform.service.TenantCatalogSyncService;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationSubscriptionServiceImpl implements OrganizationSubscriptionService {

    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PromotionRepository promotionRepository;
    private final OrganizationPromotionRepository orgPromotionRepository;
    private final SubscriptionFeatureOverrideRepository featureOverrideRepository;
    private final FeatureRepository featureRepository;
    private final TenantCatalogSyncService tenantCatalogSyncService;

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationSubscriptionResponse> getSubscriptions(SubscriptionStatus status, String search, Pageable pageable) {
        return subscriptionRepository.searchSubscriptions(status, search, pageable)
                .map(s -> toResponse(s, false));
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationSubscriptionResponse getSubscriptionById(Long id) {
        return toResponse(findById(id), true);
    }

    @Override
    @Transactional
    public OrganizationSubscriptionResponse createSubscription(OrganizationSubscriptionRequest request) {
        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + request.getOrganizationId()));
        if (subscriptionRepository.findByOrganization_IdAndActiveTrue(org.getId()).isPresent()) {
            throw new AlreadyExistsException("Organization already has an active subscription");
        }
        SubscriptionPlan plan = planRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan not found: " + request.getSubscriptionPlanId()));

        OrganizationSubscription sub = buildSubscription(org, plan, request);
        return toResponse(subscriptionRepository.save(sub), false);
    }

    @Override
    @Transactional
    public OrganizationSubscriptionResponse updateSubscription(Long id, OrganizationSubscriptionRequest request) {
        OrganizationSubscription sub = findById(id);
        SubscriptionPlan plan = planRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan not found: " + request.getSubscriptionPlanId()));
        sub.setSubscriptionPlan(plan);
        sub.setStartDate(request.getStartDate());
        sub.setEndDate(request.getEndDate());
        sub.setTrialEndDate(request.getTrialEndDate());
        sub.setBillingCycle(request.getBillingCycle());
        sub.setStudentLimitOverride(request.getStudentLimitOverride());
        sub.setStaffLimitOverride(request.getStaffLimitOverride());
        sub.setBranchLimitOverride(request.getBranchLimitOverride());
        sub.setStorageLimitOverride(request.getStorageLimitOverride());
        if (request.getAutoRenew() != null) sub.setAutoRenew(request.getAutoRenew());
        sub.setRemarks(request.getRemarks());
        return toResponse(subscriptionRepository.save(sub), false);
    }

    @Override
    @Transactional
    public OrganizationSubscriptionResponse renewSubscription(Long id, SubscriptionChangeRequest request) {
        OrganizationSubscription sub = findById(id);
        LocalDate newStart = request.getEffectiveDate() != null ? request.getEffectiveDate() : sub.getEndDate().plusDays(1);
        LocalDate newEnd = calculateEndDate(newStart, request.getBillingCycle() != null ? request.getBillingCycle() : sub.getBillingCycle());
        sub.setStartDate(newStart);
        sub.setEndDate(newEnd);
        if (request.getBillingCycle() != null) sub.setBillingCycle(request.getBillingCycle());
        sub.setStatus(SubscriptionStatus.ACTIVE);
        return toResponse(subscriptionRepository.save(sub), false);
    }

    @Override
    @Transactional
    public OrganizationSubscriptionResponse upgradeSubscription(Long id, SubscriptionChangeRequest request) {
        OrganizationSubscription sub = findById(id);
        SubscriptionPlan newPlan = planRepository.findById(request.getNewPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan not found: " + request.getNewPlanId()));
        sub.setSubscriptionPlan(newPlan);
        if (request.getBillingCycle() != null) sub.setBillingCycle(request.getBillingCycle());
        if (request.getRemarks() != null) sub.setRemarks(request.getRemarks());
        log.info("Subscription upgraded to plan: {}", newPlan.getPlanCode());
        return toResponse(subscriptionRepository.save(sub), false);
    }

    @Override
    @Transactional
    public OrganizationSubscriptionResponse downgradeSubscription(Long id, SubscriptionChangeRequest request) {
        OrganizationSubscription sub = findById(id);
        SubscriptionPlan newPlan = planRepository.findById(request.getNewPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan not found: " + request.getNewPlanId()));
        sub.setSubscriptionPlan(newPlan);
        if (request.getBillingCycle() != null) sub.setBillingCycle(request.getBillingCycle());
        if (request.getRemarks() != null) sub.setRemarks(request.getRemarks());
        log.info("Subscription downgraded to plan: {}", newPlan.getPlanCode());
        return toResponse(subscriptionRepository.save(sub), false);
    }

    @Override
    @Transactional
    public void cancelSubscription(Long id) {
        OrganizationSubscription sub = findById(id);
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setActive(false);
        subscriptionRepository.save(sub);
        log.info("Subscription cancelled for org: {}", sub.getOrganization().getOrganizationCode());
    }

    // ── Promotions ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrganizationPromotionResponse applyPromotion(OrganizationPromotionRequest request) {
        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + request.getOrganizationId()));
        Promotion promotion = promotionRepository.findById(request.getPromotionId())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found: " + request.getPromotionId()));
        if (orgPromotionRepository.existsByOrganization_IdAndPromotion_Id(org.getId(), promotion.getId())) {
            throw new AlreadyExistsException("Promotion already applied to this organization");
        }
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        OrganizationPromotion op = OrganizationPromotion.builder()
                .organization(org)
                .promotion(promotion)
                .appliedBy(currentUser)
                .applied(true)
                .active(true)
                .remarks(request.getRemarks())
                .build();
        return toPromotionResponse(orgPromotionRepository.save(op));
    }

    @Override
    @Transactional
    public void removePromotion(Long id) {
        OrganizationPromotion op = orgPromotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationPromotion not found: " + id));
        op.setActive(false);
        op.setApplied(false);
        orgPromotionRepository.save(op);
    }

    // ── Feature Overrides ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<FeatureOverrideResponse> getFeatureOverrides(Long subscriptionId) {
        findById(subscriptionId);
        return featureOverrideRepository.findByOrganizationSubscription_IdAndActiveTrueOrderByCreatedOnDesc(subscriptionId)
                .stream().map(this::toOverrideResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FeatureOverrideResponse createFeatureOverride(FeatureOverrideRequest request) {
        OrganizationSubscription sub = findById(request.getOrganizationSubscriptionId());
        Feature feature = featureRepository.findById(request.getFeatureId())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + request.getFeatureId()));
        if (featureOverrideRepository.existsByOrganizationSubscription_IdAndFeature_Id(sub.getId(), feature.getId())) {
            throw new AlreadyExistsException("Feature override already exists for this subscription");
        }
        SubscriptionFeatureOverride override = SubscriptionFeatureOverride.builder()
                .organizationSubscription(sub)
                .feature(feature)
                .enabled(request.getEnabled() == null || request.getEnabled())
                .overrideReason(request.getOverrideReason())
                .expiryDate(request.getExpiryDate())
                .complimentary(Boolean.TRUE.equals(request.getComplimentary()))
                .chargeable(Boolean.TRUE.equals(request.getChargeable()))
                .additionalCharge(request.getAdditionalCharge())
                .active(true)
                .remarks(request.getRemarks())
                .build();
        SubscriptionFeatureOverride saved = featureOverrideRepository.save(override);
        tenantCatalogSyncService.reseedOrganization(sub.getOrganization());
        return toOverrideResponse(saved);
    }

    @Override
    @Transactional
    public FeatureOverrideResponse updateFeatureOverride(Long id, FeatureOverrideRequest request) {
        SubscriptionFeatureOverride override = featureOverrideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureOverride not found: " + id));
        if (request.getEnabled() != null) override.setEnabled(request.getEnabled());
        override.setOverrideReason(request.getOverrideReason());
        override.setExpiryDate(request.getExpiryDate());
        if (request.getComplimentary() != null) override.setComplimentary(request.getComplimentary());
        if (request.getChargeable() != null) override.setChargeable(request.getChargeable());
        override.setAdditionalCharge(request.getAdditionalCharge());
        override.setRemarks(request.getRemarks());
        SubscriptionFeatureOverride saved = featureOverrideRepository.save(override);
        tenantCatalogSyncService.reseedOrganization(saved.getOrganizationSubscription().getOrganization());
        return toOverrideResponse(saved);
    }

    @Override
    @Transactional
    public void deleteFeatureOverride(Long id) {
        SubscriptionFeatureOverride override = featureOverrideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureOverride not found: " + id));
        override.setActive(false);
        featureOverrideRepository.save(override);
        tenantCatalogSyncService.reseedOrganization(override.getOrganizationSubscription().getOrganization());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OrganizationSubscription findById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationSubscription not found: " + id));
    }

    private OrganizationSubscription buildSubscription(Organization org, SubscriptionPlan plan, OrganizationSubscriptionRequest req) {
        LocalDate startDate = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        LocalDate endDate = req.getEndDate() != null ? req.getEndDate() : calculateEndDate(startDate, req.getBillingCycle());
        BigDecimal planPrice = resolvePlanPrice(plan, req.getBillingCycle());

        Promotion promotion = null;
        if (req.getPromotionId() != null) {
            promotion = promotionRepository.findById(req.getPromotionId()).orElse(null);
        }
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = planPrice;
        if (promotion != null && planPrice != null) {
            discountAmount = calculateDiscount(planPrice, promotion);
            finalAmount = planPrice.subtract(discountAmount).max(BigDecimal.ZERO);
        }

        return OrganizationSubscription.builder()
                .organization(org)
                .subscriptionPlan(plan)
                .promotion(promotion)
                .startDate(startDate)
                .endDate(endDate)
                .trialEndDate(req.getTrialEndDate())
                .billingCycle(req.getBillingCycle())
                .planPrice(planPrice)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .studentLimitOverride(req.getStudentLimitOverride())
                .staffLimitOverride(req.getStaffLimitOverride())
                .branchLimitOverride(req.getBranchLimitOverride())
                .storageLimitOverride(req.getStorageLimitOverride())
                .autoRenew(Boolean.TRUE.equals(req.getAutoRenew()))
                .status(SubscriptionStatus.ACTIVE)
                .active(true)
                .remarks(req.getRemarks())
                .build();
    }

    private BigDecimal resolvePlanPrice(SubscriptionPlan plan, com.thinkerscave.platform.enums.BillingCycle cycle) {
        if (cycle == null) return plan.getMonthlyPrice();
        return switch (cycle) {
            case MONTHLY -> plan.getMonthlyPrice();
            case QUARTERLY -> plan.getQuarterlyPrice();
            case HALF_YEARLY -> plan.getHalfYearlyPrice();
            case YEARLY -> plan.getYearlyPrice();
            default -> plan.getMonthlyPrice();
        };
    }

    private LocalDate calculateEndDate(LocalDate start, com.thinkerscave.platform.enums.BillingCycle cycle) {
        if (cycle == null) return start.plusMonths(1);
        return switch (cycle) {
            case MONTHLY -> start.plusMonths(1);
            case QUARTERLY -> start.plusMonths(3);
            case HALF_YEARLY -> start.plusMonths(6);
            case YEARLY -> start.plusYears(1);
            default -> start.plusMonths(1);
        };
    }

    private BigDecimal calculateDiscount(BigDecimal price, Promotion promotion) {
        return switch (promotion.getDiscountType()) {
            case PERCENTAGE -> price.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100));
            case FLAT_AMOUNT -> promotion.getDiscountValue().min(price);
        };
    }

    private OrganizationSubscriptionResponse toResponse(OrganizationSubscription s, boolean includeOverrides) {
        List<FeatureOverrideResponse> overrides = null;
        if (includeOverrides) {
            overrides = featureOverrideRepository.findByOrganizationSubscription_IdAndActiveTrueOrderByCreatedOnDesc(s.getId())
                    .stream().map(this::toOverrideResponse).collect(Collectors.toList());
        }
        return OrganizationSubscriptionResponse.builder()
                .id(s.getId())
                .organizationId(s.getOrganization().getId())
                .organizationName(s.getOrganization().getOrganizationName())
                .organizationCode(s.getOrganization().getOrganizationCode())
                .subscriptionPlanId(s.getSubscriptionPlan().getId())
                .planCode(s.getSubscriptionPlan().getPlanCode())
                .planName(s.getSubscriptionPlan().getPlanName())
                .promotionId(s.getPromotion() != null ? s.getPromotion().getId() : null)
                .promotionCode(s.getPromotion() != null ? s.getPromotion().getPromotionCode() : null)
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .trialEndDate(s.getTrialEndDate())
                .billingCycle(s.getBillingCycle())
                .planPrice(s.getPlanPrice())
                .discountAmount(s.getDiscountAmount())
                .finalAmount(s.getFinalAmount())
                .studentLimitOverride(s.getStudentLimitOverride())
                .staffLimitOverride(s.getStaffLimitOverride())
                .branchLimitOverride(s.getBranchLimitOverride())
                .storageLimitOverride(s.getStorageLimitOverride())
                .autoRenew(s.getAutoRenew())
                .status(s.getStatus())
                .active(s.getActive())
                .remarks(s.getRemarks())
                .featureOverrides(overrides)
                .createdOn(s.getCreatedOn())
                .createdBy(s.getCreatedBy())
                .updatedOn(s.getUpdatedOn())
                .updatedBy(s.getUpdatedBy())
                .build();
    }

    private OrganizationPromotionResponse toPromotionResponse(OrganizationPromotion op) {
        return OrganizationPromotionResponse.builder()
                .id(op.getId())
                .organizationId(op.getOrganization().getId())
                .organizationName(op.getOrganization().getOrganizationName())
                .promotionId(op.getPromotion().getId())
                .promotionCode(op.getPromotion().getPromotionCode())
                .promotionName(op.getPromotion().getPromotionName())
                .discountPercentage(op.getDiscountPercentage())
                .discountAmount(op.getDiscountAmount())
                .finalAmount(op.getFinalAmount())
                .appliedBy(op.getAppliedBy())
                .applied(op.getApplied())
                .active(op.getActive())
                .remarks(op.getRemarks())
                .createdOn(op.getCreatedOn())
                .createdBy(op.getCreatedBy())
                .build();
    }

    private FeatureOverrideResponse toOverrideResponse(SubscriptionFeatureOverride o) {
        return FeatureOverrideResponse.builder()
                .id(o.getId())
                .organizationSubscriptionId(o.getOrganizationSubscription().getId())
                .featureId(o.getFeature().getId())
                .featureCode(o.getFeature().getFeatureCode())
                .featureName(o.getFeature().getFeatureName())
                .featureKey(o.getFeature().getFeatureKey())
                .enabled(o.getEnabled())
                .overrideReason(o.getOverrideReason())
                .expiryDate(o.getExpiryDate())
                .complimentary(o.getComplimentary())
                .chargeable(o.getChargeable())
                .additionalCharge(o.getAdditionalCharge())
                .active(o.getActive())
                .remarks(o.getRemarks())
                .createdOn(o.getCreatedOn())
                .createdBy(o.getCreatedBy())
                .build();
    }
}
