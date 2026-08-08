package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.SubscriptionPlanFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPlanFeatureRepository extends JpaRepository<SubscriptionPlanFeature, Long> {

    List<SubscriptionPlanFeature> findBySubscriptionPlan_IdAndActiveTrueOrderByDisplayOrderAsc(Long planId);

    List<SubscriptionPlanFeature> findBySubscriptionPlan_IdAndEnabledTrueAndActiveTrue(Long planId);

    boolean existsBySubscriptionPlan_IdAndFeature_Id(Long planId, Long featureId);

    boolean existsBySubscriptionPlan_IdAndFeature_IdAndIdNot(Long planId, Long featureId, Long id);

    void deleteBySubscriptionPlan_Id(Long planId);
}
