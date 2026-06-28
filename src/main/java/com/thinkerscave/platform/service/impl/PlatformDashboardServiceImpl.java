package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.response.PlatformDashboardResponse;
import com.thinkerscave.platform.enums.OrganizationStatus;
import com.thinkerscave.platform.enums.PromotionStatus;
import com.thinkerscave.platform.enums.SubscriptionStatus;
import com.thinkerscave.platform.repository.CustomerRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.OrganizationSubscriptionRepository;
import com.thinkerscave.platform.repository.PromotionRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanRepository;
import com.thinkerscave.platform.service.PlatformDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformDashboardServiceImpl implements PlatformDashboardService {

    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PromotionRepository promotionRepository;

    @Override
    @Transactional(readOnly = true)
    public PlatformDashboardResponse getDashboard() {
        log.debug("Loading platform dashboard");

        long totalCustomers = customerRepository.countByActiveTrue();
        long totalOrganizations = organizationRepository.countByActiveTrue();
        long activeOrganizations = organizationRepository.countByStatus(OrganizationStatus.ACTIVE);
        long trialOrganizations = subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL);
        long suspendedOrganizations = organizationRepository.countByStatus(OrganizationStatus.SUSPENDED);
        long totalSubscriptionPlans = planRepository.countByActiveTrue();
        long activePromotions = promotionRepository.findByStatusAndActiveTrue(PromotionStatus.ACTIVE).size();

        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);
        long renewalDue = subscriptionRepository.findRenewalsDue(today, in30Days).size();

        return PlatformDashboardResponse.builder()
                .totalCustomers(totalCustomers)
                .totalOrganizations(totalOrganizations)
                .activeOrganizations(activeOrganizations)
                .trialOrganizations(trialOrganizations)
                .suspendedOrganizations(suspendedOrganizations)
                .renewalDue30Days(renewalDue)
                .provisioningInProgress(0L)
                .totalSubscriptionPlans(totalSubscriptionPlans)
                .activePromotions(activePromotions)
                .build();
    }
}
