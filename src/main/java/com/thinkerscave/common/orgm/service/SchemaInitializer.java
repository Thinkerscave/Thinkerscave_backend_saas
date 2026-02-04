package com.thinkerscave.common.orgm.service;


import com.thinkerscave.common.multitenancy.ApplicationContextService;
import com.thinkerscave.common.multitenancy.DataSourceConfig;
import com.thinkerscave.common.multitenancy.TenantContext;
import com.zaxxer.hikari.HikariDataSource;
import io.hypersistence.tsid.TSID;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import javax.xml.bind.ValidationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for initializing and managing schema-specific tables dynamically using Hibernate.
 * Used for schema-per-tenant architecture in multi-tenant setups.
 *
 * @author Sandeep
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class SchemaInitializer {

    private String sqlScript = "";

    @Value("${spring.datasource.url}")
    private String dataSourceUrl;

    @Value("${spring.datasource.username}")
    private String dataSourceUsername;

    @Value("${spring.datasource.password}")
    private String dataSourcePassword;

    @Value("${spring.datasource.driver-class-name}")
    private String dataSourceDriver;

    private final DataSource dataSource;
    private final DataSourceConfig dataSourceConfig;
    private final ApplicationContextService applicationContextService;

    @PostConstruct
    private void getSqlScript() {
        Resource resource = new ClassPathResource("schema_initializer.sql");
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
            sqlScript = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void createAndInitializeSchema(String schemaName) throws ValidationException, IOException {
        boolean exists = schemaExists(schemaName);
        if (!exists) {
            createSchema(schemaName);
        }

        DataSource newDatasource = createDataSource(
                dataSourceUrl + "?currentSchema=" + schemaName,
                dataSourceUsername,
                dataSourcePassword,
                dataSourceDriver
        );
        configureDataSource(schemaName, newDatasource);
        try {
            generateTablesFromSchemaSql(schemaName, newDatasource);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        //cleanUpSignUpThreadLocal();
    }

    private void createSchema(String schemaName) throws ValidationException {
        String sb = "CREATE SCHEMA " + schemaName;
        try (final Connection connection = dataSource.getConnection(); final PreparedStatement preparedStatement = connection.prepareStatement(sb)) {
            preparedStatement.execute();
        } catch (Exception e) {
            throw new ValidationException("Unable to create Schema", "ErrorCode.ERR_PROCESSING");
        }
    }

    public boolean schemaExists(String schemaName) {
        String sql = "SELECT 1 FROM pg_namespace WHERE nspname ='" + schemaName + "'";
        try (final Connection connection = dataSource.getConnection(); final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            final ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                final int result = resultSet.getInt(1);
                return result == 1;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void configureDataSource(String schemaName, DataSource dataSource) {
        if (Objects.isNull(dataSource)) {
            dataSource = createDataSource("%s?currentSchema=%s".formatted(dataSourceUrl, schemaName), dataSourceUsername, dataSourcePassword, dataSourceDriver);
        }
        if (saveSchemaMetaData(schemaName)) {
            applicationContextService.setNewDataSourceInAbstractRoutingDatasource(schemaName, dataSource);
        }
    }

    private boolean saveSchemaMetaData(String schemaName) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement checkStatement = connection.prepareStatement("SELECT TRUE FROM schema_meta_data WHERE schema_name = ?"); PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO schema_meta_data (id, schema_name, url, username, password) VALUES (?, ?, ?, ?, ?)")) {
            checkStatement.setString(1, schemaName);
            try (ResultSet resultSet = checkStatement.executeQuery()) {
                // Insert only if no record exists for that schema
                if (!resultSet.next()) {
                    insertStatement.setString(1, TSID.Factory.getTsid().toString());
                    insertStatement.setString(2, schemaName);
                    insertStatement.setString(3, dataSourceUrl);
                    insertStatement.setString(4, dataSourceUsername);
                    insertStatement.setString(5, dataSourcePassword);
                    return insertStatement.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return false;
    }

    public void generateTablesFromSchemaSql(String newSchema, DataSource dataSource) {
        try {
            String searchPathSql = "SET search_path TO " + newSchema;
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement setSearchPathPreparedStatement = connection.prepareStatement(searchPathSql); //Set so that we won't be getting "org.postgresql.util.PSQLException: ERROR: no schema has been selected to create in"
                 final PreparedStatement preparedStatement = connection.prepareStatement(sqlScript)) {
                setSearchPathPreparedStatement.execute();
                preparedStatement.execute();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private DataSource createDataSource(String url, String username, String password, String driver) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(url);
        hikariDataSource.setUsername(username);
        hikariDataSource.setPassword(password);
        hikariDataSource.setDriverClassName(driver);
        hikariDataSource.setMetricRegistry(dataSourceConfig.metricRegistry());
        return hikariDataSource;
    }

    private void cleanUpSignUpThreadLocal() {

    }
}
