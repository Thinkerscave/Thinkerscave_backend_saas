package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.PromotionRequest;
import com.thinkerscave.platform.dto.response.PromotionResponse;
import com.thinkerscave.platform.entity.Promotion;
import com.thinkerscave.platform.enums.PromotionStatus;
import com.thinkerscave.platform.repository.PromotionRepository;
import com.thinkerscave.platform.service.PromotionService;
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
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final CodeGeneratorService codeGeneratorService;

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> getAllPromotions() {
        return promotionRepository.findByActiveTrueOrderByCreatedOnDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request) {
        if (promotionRepository.existsByPromotionCode(request.getPromotionCode())) {
            throw new AlreadyExistsException("Promotion code already exists: " + request.getPromotionCode());
        }
        Promotion promotion = Promotion.builder()
                .promotionCode(request.getPromotionCode())
                .promotionName(request.getPromotionName())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maximumDiscount(request.getMaximumDiscount())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .maximumUsage(request.getMaximumUsage())
                .usedCount(0)
                .allowCustomPlan(request.getAllowCustomPlan() == null || request.getAllowCustomPlan())
                .stackable(Boolean.TRUE.equals(request.getStackable()))
                .autoApply(Boolean.TRUE.equals(request.getAutoApply()))
                .status(request.getStatus() != null ? request.getStatus() : PromotionStatus.ACTIVE)
                .active(true)
                .remarks(request.getRemarks())
                .build();
        return toResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        Promotion promotion = findById(id);
        if (promotionRepository.existsByPromotionCodeAndIdNot(request.getPromotionCode(), id)) {
            throw new AlreadyExistsException("Promotion code already exists: " + request.getPromotionCode());
        }
        promotion.setPromotionCode(request.getPromotionCode());
        promotion.setPromotionName(request.getPromotionName());
        promotion.setDescription(request.getDescription());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMaximumDiscount(request.getMaximumDiscount());
        promotion.setValidFrom(request.getValidFrom());
        promotion.setValidTo(request.getValidTo());
        promotion.setMaximumUsage(request.getMaximumUsage());
        if (request.getAllowCustomPlan() != null) promotion.setAllowCustomPlan(request.getAllowCustomPlan());
        if (request.getStackable() != null) promotion.setStackable(request.getStackable());
        if (request.getAutoApply() != null) promotion.setAutoApply(request.getAutoApply());
        if (request.getStatus() != null) promotion.setStatus(request.getStatus());
        promotion.setRemarks(request.getRemarks());
        return toResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public void archivePromotion(Long id) {
        Promotion promotion = findById(id);
        promotion.setActive(false);
        promotion.setStatus(PromotionStatus.DISABLED);
        promotionRepository.save(promotion);
        log.info("Promotion archived: {}", promotion.getPromotionCode());
    }

    private Promotion findById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found: " + id));
    }

    private PromotionResponse toResponse(Promotion p) {
        return PromotionResponse.builder()
                .id(p.getId())
                .promotionCode(p.getPromotionCode())
                .promotionName(p.getPromotionName())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .maximumDiscount(p.getMaximumDiscount())
                .validFrom(p.getValidFrom())
                .validTo(p.getValidTo())
                .maximumUsage(p.getMaximumUsage())
                .usedCount(p.getUsedCount())
                .allowCustomPlan(p.getAllowCustomPlan())
                .stackable(p.getStackable())
                .autoApply(p.getAutoApply())
                .status(p.getStatus())
                .active(p.getActive())
                .remarks(p.getRemarks())
                .createdOn(p.getCreatedOn())
                .createdBy(p.getCreatedBy())
                .updatedOn(p.getUpdatedOn())
                .updatedBy(p.getUpdatedBy())
                .build();
    }
}
