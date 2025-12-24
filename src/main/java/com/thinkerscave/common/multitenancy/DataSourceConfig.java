package com.thinkerscave.common.multitenancy;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String dataSourceUrl;

    @Value("${spring.datasource.username}")
    private String dataSourceUsername;

    @Value("${spring.datasource.password}")
    private String dataSourcePassword;

    @Value("${spring.datasource.driver-class-name}")
    private String dataSourceDriver;

    @Bean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    public DataSource dataSource() {
        MultiTenantDataSource multiTenantDataSource = new MultiTenantDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();

        DataSource defaultDS = createDataSource(dataSourceUrl, dataSourceUsername, dataSourcePassword, dataSourceDriver);
        targetDataSources.put("default", defaultDS);

        // fetch all records from schemaMetaData table & store it in targetDataSources map
        addExistingDataSourcesToTarget(defaultDS, targetDataSources);
        multiTenantDataSource.setTargetDataSources(targetDataSources);//Add list of datasources to Abstract Routing Datasource
        multiTenantDataSource.setDefaultTargetDataSource(defaultDS); // Set a default data source

        multiTenantDataSource.setTargetDatasourcesForHouseKeeping(targetDataSources);

        return multiTenantDataSource;

    }

    private HikariDataSource createDataSource(String url, String username, String password, String currentSchema, String driver) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(url + "?currentSchema=" + currentSchema);
        hikariDataSource.setUsername(username);
        hikariDataSource.setPassword(password);
        hikariDataSource.setDriverClassName(driver);
        hikariDataSource.setMetricRegistry(metricRegistry());
        return hikariDataSource;
    }


    //Default Datasource handler
    private HikariDataSource createDataSource(String url, String username, String password, String driver) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(url);
        hikariDataSource.setUsername(username);
        hikariDataSource.setPassword(password);
        hikariDataSource.setDriverClassName(driver);
        hikariDataSource.setMetricRegistry(metricRegistry());
        return hikariDataSource;
    }

    private void addExistingDataSourcesToTarget(DataSource dataSource, Map<Object, Object> targetDataSources) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM schema_meta_data");
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                String schemaName = resultSet.getString("schema_name");
                DataSource source = createDataSource(resultSet.getString("url"), resultSet.getString("username"),
                        resultSet.getString("password"), schemaName, dataSourceDriver);
                targetDataSources.put(schemaName, source);
            }
        } catch (SQLException e) {
            log.info("Only public schema exists");
        }
    }
}