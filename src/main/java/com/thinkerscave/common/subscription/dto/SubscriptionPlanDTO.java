package com.thinkerscave.common.subscription.dto;

import java.math.BigDecimal;

public record SubscriptionPlanDTO(
        Long planId,
        String planCode,
        String planName,
        String description,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        String currency,
        Integer maxStudents,
        Integer maxStaff,
        Integer maxUsers,
        Integer storageGb,
        String modulesIncluded,
        String supportTier,
        String highlightColor,
        Boolean featured,
        Boolean active
) { }
