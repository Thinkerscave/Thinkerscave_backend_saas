package com.thinkerscave.common.multitenancy;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@Log4j2
@Component
public class ApplicationContextService implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setNewDataSourceInAbstractRoutingDatasource(String schemaName, DataSource newDatasource) {
        MultiTenantDataSource multiTenantDataSource = applicationContext.getBean(MultiTenantDataSource.class);
        final Map<Object, Object> targetDatasourcesForHouseKeeping = multiTenantDataSource.getTargetDatasourcesForHouseKeeping();
        if (newDatasource != null) {
            targetDatasourcesForHouseKeeping.put(schemaName, newDatasource);
            multiTenantDataSource.setTargetDataSources(targetDatasourcesForHouseKeeping);
            multiTenantDataSource.afterPropertiesSet(); // Ensure the data source is properly refreshed
        } else {
            throw new IllegalArgumentException("Tenant ID does not exist: " + schemaName);
        }
    }

    public void deleteDataSourceFromAbstractRoutingDatasource(String schemaName, DataSource idleDataSource) {
        MultiTenantDataSource multiTenantDataSource = applicationContext.getBean(MultiTenantDataSource.class);
        final Map<Object, Object> targetDatasourcesForHouseKeeping = multiTenantDataSource.getTargetDatasourcesForHouseKeeping();
        if (targetDatasourcesForHouseKeeping.containsKey(schemaName)) {
            HikariDataSource dataSource = (HikariDataSource) targetDatasourcesForHouseKeeping.get(schemaName);
            try {
                dataSource.close();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            targetDatasourcesForHouseKeeping.remove(schemaName);
            targetDatasourcesForHouseKeeping.put(schemaName, idleDataSource);
            multiTenantDataSource.setTargetDataSources(targetDatasourcesForHouseKeeping);
            multiTenantDataSource.afterPropertiesSet(); // Ensure the data source is properly refreshed
        } else {
            log.error("TargetDatasources does not contain {} schema", schemaName);
        }
    }
}