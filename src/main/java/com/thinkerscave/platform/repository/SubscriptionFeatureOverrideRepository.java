package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.SubscriptionFeatureOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionFeatureOverrideRepository extends JpaRepository<SubscriptionFeatureOverride, Long> {

    List<SubscriptionFeatureOverride> findByOrganizationSubscription_IdAndActiveTrueOrderByCreatedOnDesc(Long subscriptionId);

    boolean existsByOrganizationSubscription_IdAndFeature_Id(Long subscriptionId, Long featureId);

    boolean existsByOrganizationSubscription_IdAndFeature_IdAndIdNot(Long subscriptionId, Long featureId, Long id);
}
