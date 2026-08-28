package com.thinkerscave.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Local MySQL schema patches AFTER Hibernate has created entity tables.
 * Master data (super admin, roles, menus, plans) is owned by {@link PlatformBootstrapSeed}.
 * Active only under the "dev" Spring profile.
 */
@Component
@Profile("dev")
@Slf4j
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Dev schema patches only - platform master data is owned by PlatformBootstrapSeed.");
        try {
            ensureCustomerOwnerColumn();
            ensurePlatformSchema();
            ensureAdmissionsCrmSchema();
            ensureResponsibilityPermissionsSchema();
            log.info("Dev schema patches applied.");
        } catch (Exception e) {
            log.warn("Dev schema patch issue: {}", e.getMessage());
        }
    }

    private void ensureCustomerOwnerColumn() {
        ensureCustomerOwnerColumn(null);
    }

    private void ensureCustomerOwnerColumn(String schema) {
        String tableRef = (schema == null || schema.isBlank()) ? "customers" : ("`" + schema + "`.`customers`");
        try (Connection connection = dataSource.getConnection()) {
            String catalog = (schema == null || schema.isBlank()) ? connection.getCatalog() : schema;
            boolean exists;
            try (ResultSet columns = connection.getMetaData().getColumns(catalog, null, "customers", "owner_user_id")) {
                exists = columns.next();
            }

            if (!exists) {
                jdbcTemplate.execute("ALTER TABLE " + tableRef + " ADD COLUMN owner_user_id BIGINT NULL");
                log.info("Applied fallback migration: {}.owner_user_id added", tableRef);
            }

            try {
                if (schema == null || schema.isBlank()) {
                    jdbcTemplate.execute("CREATE INDEX idx_customer_owner ON customers (owner_user_id)");
                } else {
                    jdbcTemplate.execute("CREATE INDEX idx_customer_owner ON " + tableRef + " (owner_user_id)");
                }
            } catch (Exception ignored) {
                // Ignore duplicate-index or unsupported-if-exists differences across engines.
            }
        } catch (Exception ex) {
            log.warn("Could not ensure {} owner_user_id migration: {}", tableRef, ex.getMessage());
        }
    }

    /**
     * Flyway is disabled on local MySQL. Keep platform columns in sync on every catalog
     * so org-select / login do not fail with Unknown column errors on cloned tenant DBs.
     */
    private void ensurePlatformSchema() {
        List<String> schemas = new ArrayList<>();
        schemas.add("thinkerscave_dev");
        try {
            jdbcTemplate.queryForList(
                            "SELECT schema_name FROM tenant_registry WHERE schema_name IS NOT NULL AND TRIM(schema_name) <> ''",
                            String.class)
                    .forEach(schemas::add);
        } catch (Exception ignored) {
            // tenant registry may not exist yet
        }
        for (String schema : schemas.stream().distinct().toList()) {
            ensureColumn(schema, "organizations", "admin_full_name", "varchar(200) NULL");
            ensureColumn(schema, "tenant_registry", "student_count", "INT NULL");
            ensureColumn(schema, "tenant_registry", "staff_count", "INT NULL");
            ensureColumn(schema, "tenant_registry", "branch_count", "INT NULL");
            ensureColumn(schema, "tenant_registry", "class_count", "INT NULL");
            ensureColumn(schema, "tenant_registry", "section_count", "INT NULL");
            ensureColumn(schema, "tenant_registry", "usage_refreshed_at", "datetime NULL");
        }
    }

    private void ensureAdmissionsCrmSchema() {
        List<String> schemas = new ArrayList<>();
        schemas.add("thinkerscave_dev");
        try {
            jdbcTemplate.queryForList(
                            "SELECT schema_name FROM tenant_registry WHERE schema_name IS NOT NULL AND TRIM(schema_name) <> ''",
                            String.class)
                    .forEach(schemas::add);
        } catch (Exception ignored) {
            // tenant registry may not exist yet
        }
        for (String schema : schemas.stream().distinct().toList()) {
            try {
                ensureColumn(schema, "inquiry", "inquiry_number", "varchar(40) NULL");
                ensureColumn(schema, "inquiry", "academic_year_id", "bigint NULL");
                ensureColumn(schema, "inquiry", "class_id", "bigint NULL");
                ensureColumn(schema, "inquiry_follow_up", "lifecycle_status", "varchar(20) NULL DEFAULT 'SCHEDULED'");
                ensureColumn(schema, "inquiry_follow_up", "outcome", "varchar(200) NULL");
                ensureColumn(schema, "inquiry_follow_up", "completed_on", "datetime NULL");
                ensureColumn(schema, "inquiry_follow_up", "completed_by", "varchar(100) NULL");
                ensureColumn(schema, "application_admission", "academic_year_id", "bigint NULL");
                ensureColumn(schema, "application_admission", "class_id", "bigint NULL");
                ensureColumn(schema, "application_admission", "section_id", "bigint NULL");
                ensureColumn(schema, "application_admission", "student_id", "bigint NULL");
                ensureColumn(schema, "application_admission", "fee_amount", "decimal(12,2) NULL");
                ensureColumn(schema, "application_admission", "fee_receipt_number", "varchar(60) NULL");
                ensureColumn(schema, "application_admission", "fee_payment_mode", "varchar(40) NULL");
                ensureColumn(schema, "application_admission", "fee_paid_on", "date NULL");
                ensureColumn(schema, "application_admission", "fee_received_by", "varchar(100) NULL");
                ensureColumn(schema, "application_admission", "fee_remarks", "text NULL");
                ensureColumn(schema, "application_admission", "fee_status", "varchar(20) NULL DEFAULT 'PENDING'");
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS `%s`.`admissions_setting` (
                          setting_id BIGINT NOT NULL AUTO_INCREMENT,
                          organization_id BIGINT NOT NULL,
                          inquiry_sources TEXT,
                          inquiry_statuses TEXT,
                          required_documents TEXT,
                          lead_prefix VARCHAR(20) DEFAULT 'LD',
                          application_prefix VARCHAR(20) DEFAULT 'APP',
                          admission_prefix VARCHAR(20) DEFAULT 'ADM',
                          reminder_mode VARCHAR(30) DEFAULT 'AUTO',
                          reminder_lead_time VARCHAR(20) DEFAULT '24H',
                          assignment_mode VARCHAR(30) DEFAULT 'MANUAL',
                          created_by VARCHAR(100),
                          created_on DATETIME,
                          updated_by VARCHAR(100),
                          updated_on DATETIME,
                          version BIGINT NOT NULL DEFAULT 0,
                          PRIMARY KEY (setting_id),
                          UNIQUE KEY uq_adm_setting_org (organization_id)
                        )
                        """.formatted(schema));
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS `%s`.`admission_application_document` (
                          document_id BIGINT NOT NULL AUTO_INCREMENT,
                          application_id BIGINT NOT NULL,
                          document_type VARCHAR(80) NOT NULL,
                          original_name VARCHAR(255),
                          stored_path VARCHAR(500),
                          status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                          remarks TEXT,
                          created_by VARCHAR(100),
                          created_on DATETIME,
                          updated_by VARCHAR(100),
                          updated_on DATETIME,
                          version BIGINT NOT NULL DEFAULT 0,
                          PRIMARY KEY (document_id),
                          KEY idx_aad_application (application_id)
                        )
                        """.formatted(schema));
            } catch (Exception ex) {
                log.warn("Could not patch admissions schema on {}: {}", schema, ex.getMessage());
            }
        }
    }

    private void ensureResponsibilityPermissionsSchema() {
        List<String> schemas = new ArrayList<>();
        schemas.add("thinkerscave_dev");
        try {
            jdbcTemplate.queryForList(
                            "SELECT schema_name FROM tenant_registry WHERE schema_name IS NOT NULL AND TRIM(schema_name) <> ''",
                            String.class)
                    .forEach(schemas::add);
        } catch (Exception ignored) {
            // tenant registry may not exist yet
        }
        for (String schema : schemas.stream().distinct().toList()) {
            try {
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS `%s`.`responsibility_permissions` (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          organization_id BIGINT NOT NULL,
                          responsibility_id BIGINT NOT NULL,
                          menu_id BIGINT NOT NULL,
                          can_view TINYINT(1) DEFAULT 0,
                          can_manage TINYINT(1) DEFAULT 0,
                          can_approve TINYINT(1) DEFAULT 0,
                          created_by VARCHAR(100),
                          created_on DATETIME,
                          updated_by VARCHAR(100),
                          updated_on DATETIME,
                          version BIGINT NOT NULL DEFAULT 0,
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_responsibility_permission (organization_id, responsibility_id, menu_id),
                          KEY idx_resp_permission_resp (responsibility_id),
                          KEY idx_resp_permission_menu (menu_id)
                        )
                        """.formatted(schema));
            } catch (Exception ex) {
                log.warn("Could not patch responsibility_permissions on {}: {}", schema, ex.getMessage());
            }
        }
    }

    private void ensureColumn(String schema, String table, String column, String ddlType) {
        try (Connection connection = dataSource.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(schema, null, table, column)) {
            if (columns.next()) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE `" + schema + "`.`" + table + "` ADD COLUMN `" + column + "` " + ddlType);
            log.info("Added {}.{} .{}", schema, table, column);
        } catch (Exception ex) {
            log.debug("Could not add {}.{} .{}: {}", schema, table, column, ex.getMessage());
        }
    }
}
