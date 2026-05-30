package com.thinkerscave.common.dashboard.repository;

import com.thinkerscave.common.dashboard.domain.DashboardWidgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DashboardWidgetConfigRepository extends JpaRepository<DashboardWidgetConfig, Long> {

    List<DashboardWidgetConfig> findByRoleCodeInAndEnabledTrue(Collection<String> roleCodes);
}