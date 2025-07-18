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

@Service
public class SchemaInitializer {

    private final DataSource dataSource;

    private static final Map<String, Class<?>> TABLE_ENTITY_MAP = Map.of(
            "user", com.thinkerscave.common.usrm.domain.User.class,
            "passwordresettoken", com.thinkerscave.common.usrm.domain.PasswordResetToken.class,
            "refreshtoken", com.thinkerscave.common.usrm.domain.RefreshToken.class,
            "role", com.thinkerscave.common.role.domain.Role.class,
            "organisation", com.thinkerscave.common.orgm.domain.Organisation.class,
            "ownerdetails", com.thinkerscave.common.orgm.domain.OwnerDetails.class,
            "menu", com.thinkerscave.common.menum.domain.Menu.class,
            "submenu_master",com.thinkerscave.common.menum.domain.Submenu.class
    );

    public SchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createTablesForSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection()) {

            // Set the current schema for the connection
            connection.createStatement().execute("SET search_path TO " + schemaName);

            // Build Hibernate ServiceRegistry with connection
            Map<String, Object> settings = new HashMap<>();
            settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            settings.put("hibernate.hbm2ddl.auto", "none");

            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .applySettings(settings)
                    .addService(ConnectionProvider.class, new SingleConnectionProvider(connection))
                    .build();

            // Load only the required entities dynamically
            Metadata metadata = new MetadataSources(registry)
                    .addAnnotatedClass(com.thinkerscave.common.usrm.domain.User.class)
                    .addAnnotatedClass(com.thinkerscave.common.usrm.domain.PasswordResetToken.class)
                    .addAnnotatedClass(com.thinkerscave.common.usrm.domain.RefreshToken.class)
                    .addAnnotatedClass(com.thinkerscave.common.role.domain.Role.class)
                    .addAnnotatedClass(com.thinkerscave.common.orgm.domain.Organisation.class)
                    .addAnnotatedClass(com.thinkerscave.common.orgm.domain.OwnerDetails.class)
                    .addAnnotatedClass(com.thinkerscave.common.menum.domain.Menu.class)
                    .addAnnotatedClass(com.thinkerscave.common.menum.domain.Submenu.class)


                    // Add more .addAnnotatedClass(...) as needed
                    .buildMetadata();

            // Use SchemaExport to create tables in the DB for the schema
            SchemaExport export = new SchemaExport();
            export.setDelimiter(";");
            export.setFormat(true);
            export.create(EnumSet.of(TargetType.DATABASE), metadata);

        } catch (SQLException e) {
            throw new RuntimeException("Error creating tables in schema: " + schemaName, e);
        }
    }

    public void createMissingTablesInSchema(String schemaName, List<String> missingTables) {
        try (Connection connection = dataSource.getConnection()) {

            // Set schema
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
