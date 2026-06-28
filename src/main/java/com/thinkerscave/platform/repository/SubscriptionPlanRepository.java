package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    boolean existsByPlanCode(String planCode);

    boolean existsByPlanName(String planName);

    boolean existsByPlanCodeAndIdNot(String planCode, Long id);

    boolean existsByPlanNameAndIdNot(String planName, Long id);

    Optional<SubscriptionPlan> findByPlanCode(String planCode);

    List<SubscriptionPlan> findByActiveTrueOrderByDisplayOrderAsc();

    long countByActiveTrue();
}
