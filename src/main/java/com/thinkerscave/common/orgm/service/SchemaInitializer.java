package com.thinkerscave.common.orgm.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SchemaInitializer - Creates and initializes tenant schemas.
 * 
 * For Approach 3 (All Tables Per Tenant), this service:
 * 1. Creates a new PostgreSQL schema for each tenant
 * 2. Lets Hibernate auto-generate tables via ddl-auto=update
 * 3. Seeds default data (menus, privileges, roles)
 */
@Service
public class SchemaInitializer {

    private final DataSource dataSource;

    @Value("${spring.datasource.url}")
    private String dataSourceUrl;

    public SchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initializeGlobalTables() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            // 1. Create tenant_config
            statement.execute("""
                        CREATE TABLE IF NOT EXISTS public.tenant_config (
                            id BIGSERIAL PRIMARY KEY,
                            tenant_id VARCHAR(255) UNIQUE NOT NULL,
                            tenant_name VARCHAR(500),
                            subdomain VARCHAR(255) UNIQUE,
                            custom_domain VARCHAR(255),
                            is_active BOOLEAN DEFAULT true,
                            max_users INTEGER DEFAULT 100,
                            storage_limit_mb INTEGER DEFAULT 10240,
                            features JSONB DEFAULT '{}'::jsonb,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            created_by VARCHAR(255),
                            CONSTRAINT tenant_id_format CHECK (tenant_id ~ '^[a-z0-9_]{3,50}$')
                        )
                    """);

            // 2. Create tenant_audit_log
            statement.execute("""
                        CREATE TABLE IF NOT EXISTS public.tenant_audit_log (
                            id BIGSERIAL PRIMARY KEY,
                            tenant_id VARCHAR(255) NOT NULL,
                            action VARCHAR(50) NOT NULL,
                            performed_by VARCHAR(255),
                            performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            details JSONB,
                            ip_address VARCHAR(45),
                            status VARCHAR(20) DEFAULT 'SUCCESS',
                            error_message TEXT,
                            duration_ms INTEGER,
                            CONSTRAINT valid_status CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING'))
                        )
                    """);

            // 3. Create indexes
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_tenant_config_subdomain ON public.tenant_config(subdomain)");
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_tenant_config_active ON public.tenant_config(is_active)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_audit_tenant ON public.tenant_audit_log(tenant_id)");

            // 4. Create organization_users (Needed for Super Admin in public schema)
            statement.execute("""
                        CREATE TABLE IF NOT EXISTS public.organization_users (
                            id BIGSERIAL PRIMARY KEY,
                            organization_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            role_name VARCHAR(50),
                            is_active BOOLEAN DEFAULT true,
                            joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_org_users_user ON public.organization_users(user_id)");
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_org_users_org ON public.organization_users(organization_id)");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize global tables", e);
        }
    }

    /**
     * Creates a new schema for a tenant if it doesn't exist.
     * Also copies all table structures from public schema.
     * 
     * @param schemaName The name of the schema (e.g., "school_abc")
     * @return true if schema was created, false if it already existed
     */
    public boolean createSchemaIfNotExists(String schemaName) throws SQLException {
        // Sanitize schema name to prevent SQL injection
        String sanitizedSchema = sanitizeSchemaName(schemaName);

        try (Connection connection = dataSource.getConnection()) {
            // Check if schema exists
            if (schemaExists(sanitizedSchema, connection)) {
                return false;
            }

            // Create the schema
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA \"" + sanitizedSchema + "\"");
            }

            // Copy table structures from public schema
            copyTablesFromPublic(sanitizedSchema, connection);

            return true;
        }
    }

    /**
     * Copies all table structures from public schema to the target schema.
     * This ensures new tenants have all required tables.
     */
    // Tables that should only exist in the public schema, not in tenant schemas
    private static final java.util.Set<String> PUBLIC_ONLY_TABLES = java.util.Set.of(
            "tenant_config", "tenant_audit_log", "user_tenant_mapping",
            "organisation", "owner_details");

    /**
     * Copies table structures from public schema to the target schema,
     * excluding tables that should only exist in the public schema.
     */
    private void copyTablesFromPublic(String targetSchema, Connection connection) throws SQLException {
        // Get list of tables from public schema
        String getTablesQuery = "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'";

        java.util.List<String> tables = new java.util.ArrayList<>();
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(getTablesQuery)) {
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                if (!PUBLIC_ONLY_TABLES.contains(tableName)) {
                    tables.add(tableName);
                }
            }
        }

        // Copy each table structure to the new schema
        for (String tableName : tables) {
            String copyTableSql = String.format(
                    "CREATE TABLE \"%s\".\"%s\" (LIKE public.\"%s\" INCLUDING ALL)",
                    targetSchema, tableName, tableName);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(copyTableSql);
            }
        }
    }

    /**
     * Checks if a schema exists in the database.
     */
    public boolean schemaExists(String schemaName) throws SQLException {
        String sanitizedSchema = sanitizeSchemaName(schemaName);
        try (Connection connection = dataSource.getConnection()) {
            return schemaExists(sanitizedSchema, connection);
        }
    }

    private boolean schemaExists(String schemaName, Connection connection) throws SQLException {
        String query = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?";
        try (java.sql.PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, schemaName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Drops a schema and all its contents.
     * Use with caution - this is irreversible!
     */
    public void dropSchema(String schemaName) throws SQLException {
        String sanitizedSchema = sanitizeSchemaName(schemaName);
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS \"" + sanitizedSchema + "\" CASCADE");
        }
    }

    /**
     * Lists all tenant schemas (excludes system schemas).
     */
    public java.util.List<String> listTenantSchemas() throws SQLException {
        java.util.List<String> schemas = new java.util.ArrayList<>();
        String query = "SELECT schema_name FROM information_schema.schemata " +
                "WHERE schema_name NOT IN ('pg_catalog', 'information_schema', 'pg_toast', 'public')";

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                schemas.add(rs.getString("schema_name"));
            }
        }
        return schemas;
    }

    /**
     * Seeds an initial user into the tenant schema.
     * Also syncs to public.user_tenant_mapping for auto-tenant detection.
     * 
     * @param schemaName The tenant schema
     * @param username   Username (email)
     * @param password   Hashed Password
     * @param roleCode   The role code (e.g., ADMIN, IT_SUPPORT)
     * @param firstName  User's first name
     * @param lastName   User's last name
     */
    public void seedTenantUser(String schemaName, String username, String password, String roleCode, String firstName,
            String lastName) throws SQLException {
        String sanitizedSchema = sanitizeSchemaName(schemaName);

        try (Connection connection = dataSource.getConnection()) {
            if (!schemaExists(sanitizedSchema, connection)) {
                throw new java.sql.SQLException("Schema " + schemaName + " does not exist");
            }

            // 1. Ensure Role exists
            long roleId = ensureRoleExists(sanitizedSchema, connection, roleCode);

            // 2. Insert User
            long userId = insertUser(sanitizedSchema, connection, username, password, firstName, lastName);

            // 3. Map User to Role
            assignRoleToUser(sanitizedSchema, connection, userId, roleId);

            // 4. CRITICAL: Sync to user_tenant_mapping for auto-tenant detection
            syncToUserTenantMapping(sanitizedSchema, connection, username);
        }
    }

    /**
     * Syncs user to public.user_tenant_mapping table.
     * This enables auto-tenant detection during login.
     */
    private void syncToUserTenantMapping(String tenantId, Connection conn, String username) throws SQLException {
        String sql = """
                INSERT INTO public.user_tenant_mapping
                (email, username, tenant_id, is_active, created_at, updated_at)
                VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (email)
                DO UPDATE SET
                    is_active = true,
                    updated_at = CURRENT_TIMESTAMP,
                    username = EXCLUDED.username
                """;

        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username); // email
            ps.setString(2, username); // username (same as email for now)
            ps.setString(3, tenantId);
            ps.executeUpdate();
        }
    }

    private long ensureRoleExists(String schema, Connection conn, String roleCode) throws SQLException {
        String query = String.format("SELECT role_id FROM \"%s\".role_master WHERE role_code = ?", schema);
        try (java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, roleCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("role_id");
                }
            }
        }

        // Create if not exists
        String insertSql = String.format(
                "INSERT INTO \"%s\".role_master (role_name, role_code, is_active, role_type, created_date) VALUES (?, ?, true, 'ADMIN', CURRENT_TIMESTAMP) RETURNING role_id",
                schema);
        try (java.sql.PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, roleCode);
            ps.setString(2, roleCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("role_id");
                }
            }
        }
        throw new SQLException("Failed to create role");
    }

    private long insertUser(String schema, Connection conn, String username, String password, String firstName,
            String lastName) throws SQLException {
        // Check if user exists
        String checkSql = String.format("SELECT id FROM \"%s\".users WHERE user_name = ?", schema);
        try (java.sql.PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }

        // Insert user
        String insertSql = String.format(
                "INSERT INTO \"%s\".users (user_name, password, email, user_code, first_name, last_name, mobile_number, is_blocked, is_first_time_login, is_email_verified, is_mobile_verified, created_date) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, 1234567890, false, false, true, true, CURRENT_TIMESTAMP) RETURNING id",
                schema);

        try (java.sql.PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, username);
            ps.setString(4, "USR-" + System.currentTimeMillis());
            ps.setString(5, firstName);
            ps.setString(6, lastName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        throw new SQLException("Failed to create user");
    }

    private void assignRoleToUser(String schema, Connection conn, long userId, long roleId) throws SQLException {
        // Check mapping
        String checkSql = String.format("SELECT 1 FROM \"%s\".user_roles WHERE user_id = ? AND role_id = ?", schema);
        try (java.sql.PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setLong(1, userId);
            ps.setLong(2, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return;
            }
        }

        // Insert mapping
        String insertSql = String.format("INSERT INTO \"%s\".user_roles (user_id, role_id) VALUES (?, ?)", schema);
        try (java.sql.PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setLong(1, userId);
            ps.setLong(2, roleId);
            ps.execute();
        }
    }

    public java.util.List<java.util.Map<String, Object>> debugListUsers(String schemaName, String username)
            throws SQLException {
        String sanitizedSchema = sanitizeSchemaName(schemaName);
        java.util.List<java.util.Map<String, Object>> users = new java.util.ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            if (!schemaExists(sanitizedSchema, connection)) {
                return users;
            }

            String sql = "SELECT * FROM \"" + sanitizedSchema + "\".users WHERE user_name = ?";
            try (java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    java.sql.ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    while (rs.next()) {
                        java.util.Map<String, Object> row = new java.util.HashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        users.add(row);
                    }
                }
            }
        }
        return users;
    }

    /**
     * Sanitizes schema name to prevent SQL injection.
     * Only allows alphanumeric characters and underscores.
     */
    private String sanitizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }
        return schemaName.toLowerCase().replaceAll("[^a-z0-9_]", "");
    }
}