package com.thinkerscave.common.multitenancy;

import com.thinkerscave.common.config.TenantContext;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Log4j2
    public class MultiTenantDataSource extends AbstractRoutingDataSource {

    public static final String BIPROS_DEFAULT_ADMINISTRATOR = "Bipros Default Administrator";
    private Map<Object, Object> targetDatasourcesForHouseKeeping = new HashMap<>();

    public Map<Object, Object> getTargetDatasourcesForHouseKeeping() {
        return targetDatasourcesForHouseKeeping;
    }

    public void setTargetDatasourcesForHouseKeeping(Map<Object, Object> targetDatasourcesForHouseKeeping) {
        this.targetDatasourcesForHouseKeeping = targetDatasourcesForHouseKeeping;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // Use a strategy to determine the current user or tenant identifier
        String currentUserOrTenant = determineCurrentUserOrTenant();
        log.info("currentUserOrTenant:-- {}", currentUserOrTenant);
        return currentUserOrTenant;
    }

    private String determineCurrentUserOrTenant() {
        String currentTenant = TenantContext.getTenant();
        log.info("CurrentTenant#########:-- {}", currentTenant);
        if (Objects.nonNull(currentTenant)){
            return currentTenant;
        }
        return "default"; // Replace with actual logic
    }
}