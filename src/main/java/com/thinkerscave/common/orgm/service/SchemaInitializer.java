package com.thinkerscave.common.orgm.service;


import com.thinkerscave.common.config.SingleConnectionProvider;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for initializing and managing schema-specific tables dynamically using Hibernate.
 * Used for schema-per-tenant architecture in multi-tenant setups.
 *
 * @author Sandeep
 */
@Service
public class SchemaInitializer {

    private final DataSource dataSource;

    private static final Map<String, Class<?>> TABLE_ENTITY_MAP = Map.ofEntries(
            Map.entry("user", com.thinkerscave.common.usrm.domain.User.class),
            Map.entry("passwordresettoken", com.thinkerscave.common.usrm.domain.PasswordResetToken.class),
            Map.entry("refreshtoken", com.thinkerscave.common.usrm.domain.RefreshToken.class),
            Map.entry("role", com.thinkerscave.common.role.domain.Role.class),
            Map.entry("organisation", com.thinkerscave.common.orgm.domain.Organisation.class),
            Map.entry("ownerdetails", com.thinkerscave.common.orgm.domain.OwnerDetails.class),
            Map.entry("menu", com.thinkerscave.common.menum.domain.Menu.class),
            Map.entry("submenu_master", com.thinkerscave.common.menum.domain.Submenu.class),
            Map.entry("branch", com.thinkerscave.common.staff.domain.Branch.class),
            Map.entry("department", com.thinkerscave.common.staff.domain.Department.class),
            Map.entry("staff", com.thinkerscave.common.staff.domain.Staff.class)
    );



    public SchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Creates all required tables for a given schema. */
    public void createTablesForSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("SET search_path TO " + schemaName);

            Map<String, Object> settings = new HashMap<>();
            settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            settings.put("hibernate.hbm2ddl.auto", "none");

            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .applySettings(settings)
                    .addService(ConnectionProvider.class, new SingleConnectionProvider(connection))
                    .build();

            Metadata metadata = new MetadataSources(registry)
                    .addAnnotatedClass(com.thinkerscave.common.usrm.domain.User.class)
                    .addAnnotatedClass(com.thinkerscave.common.usrm.domain.PasswordResetToken.class)
                    .addAnnotatedClass(com.thinkerscave.common.usrm.domain.RefreshToken.class)
                    .addAnnotatedClass(com.thinkerscave.common.role.domain.Role.class)
                    .addAnnotatedClass(com.thinkerscave.common.orgm.domain.Organisation.class)
                    .addAnnotatedClass(com.thinkerscave.common.orgm.domain.OwnerDetails.class)
                    .addAnnotatedClass(com.thinkerscave.common.menum.domain.Menu.class)
                    .addAnnotatedClass(com.thinkerscave.common.menum.domain.Submenu.class)
                    .addAnnotatedClass(com.thinkerscave.common.staff.domain.Branch.class)
                    .addAnnotatedClass(com.thinkerscave.common.staff.domain.Department.class)
                    .addAnnotatedClass(com.thinkerscave.common.staff.domain.Staff.class)
                    .addAnnotatedClass(com.thinkerscave.common.admission.domain.ApplicationAdmission.class)
                    .buildMetadata();

            SchemaExport export = new SchemaExport();
            export.setDelimiter(";");
            export.setFormat(true);
            export.create(EnumSet.of(TargetType.DATABASE), metadata);

        } catch (SQLException e) {
            throw new RuntimeException("Error creating tables in schema: " + schemaName, e);
        }
    }

    /** Creates only the missing tables in a given schema. */
    public void createMissingTablesInSchema(String schemaName, List<String> missingTables) {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("SET search_path TO " + schemaName);

            Map<String, Object> settings = new HashMap<>();
            settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            settings.put("hibernate.hbm2ddl.auto", "none");

            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .applySettings(settings)
                    .addService(ConnectionProvider.class, new SingleConnectionProvider(connection))
                    .build();

            MetadataSources sources = new MetadataSources(registry);

            for (String tableName : missingTables) {
                Class<?> entityClass = TABLE_ENTITY_MAP.get(tableName.toLowerCase());
                if (entityClass != null) {
                    sources.addAnnotatedClass(entityClass);
                }
            }

            Metadata metadata = sources.buildMetadata();

            SchemaExport export = new SchemaExport();
            export.setDelimiter(";");
            export.setFormat(true);
            export.create(EnumSet.of(TargetType.DATABASE), metadata);

        } catch (SQLException e) {
            throw new RuntimeException("Error creating tables in schema: " + schemaName, e);
        }
    }
}