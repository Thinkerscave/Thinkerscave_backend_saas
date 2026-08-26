package com.thinkerscave.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * Flyway is disabled on the shared PostgreSQL test/prod server. Hibernate ddl-auto
 * only updates the schema that is current at startup (usually public), so tenant
 * schemas lag behind entity changes. Apply the admissions CRM and session tables
 * to public + tenant_* on every boot. No demo rows are inserted.
 */
@Component
@Profile({"test", "prod"})
@Order(50)
@Slf4j
@RequiredArgsConstructor
public class PostgresTenantSchemaPatcher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String crmSql = StreamUtils.copyToString(
                    new ClassPathResource("db/migration/V1_33__admissions_crm_production_columns.sql").getInputStream(),
                    StandardCharsets.UTF_8);
            jdbcTemplate.execute(crmSql);
            jdbcTemplate.execute(SESSION_AND_NOTES_SQL);
            log.info("PostgreSQL tenant schema patch applied (admissions CRM + user_sessions + counseling_note).");
        } catch (Exception ex) {
            log.warn("PostgreSQL tenant schema patch failed: {}", ex.getMessage());
        }
    }

    private static final String SESSION_AND_NOTES_SQL = """
            DO $$
            DECLARE
                s text;
            BEGIN
                FOR s IN
                    SELECT nspname
                    FROM pg_namespace
                    WHERE nspname = 'public'
                       OR nspname LIKE 'tenant_%'
                LOOP
                    EXECUTE format(
                        'CREATE TABLE IF NOT EXISTS %I.user_sessions (
                            id bigserial PRIMARY KEY,
                            user_id bigint NOT NULL,
                            refresh_token varchar(500) NOT NULL,
                            device_name varchar(200),
                            browser varchar(100),
                            operating_system varchar(100),
                            ip_address varchar(100),
                            login_at timestamp,
                            logout_at timestamp,
                            status varchar(30),
                            created_by varchar(100),
                            created_on timestamp,
                            updated_by varchar(100),
                            updated_on timestamp,
                            version bigint NOT NULL DEFAULT 0
                        )', s);
                    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_session_user ON %I.user_sessions (user_id)', s);
                    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_session_token ON %I.user_sessions (refresh_token)', s);

                    EXECUTE format(
                        'CREATE TABLE IF NOT EXISTS %I.counseling_note (
                            note_id bigserial PRIMARY KEY,
                            inquiry_id bigint NOT NULL,
                            student_requirements text,
                            parent_concerns text,
                            campus_visit_info text,
                            recommendations text,
                            notes text,
                            created_by varchar(100),
                            created_on timestamp,
                            updated_by varchar(100),
                            updated_on timestamp,
                            version bigint NOT NULL DEFAULT 0
                        )', s);
                    EXECUTE format(
                        'CREATE TABLE IF NOT EXISTS %I.application_documents (
                            application_id bigint NOT NULL,
                            document_url varchar(500)
                        )', s);

                    EXECUTE format(
                        'CREATE TABLE IF NOT EXISTS %I.responsibility_permissions (
                            id bigserial PRIMARY KEY,
                            organization_id bigint NOT NULL,
                            responsibility_id bigint NOT NULL,
                            menu_id bigint NOT NULL,
                            can_view boolean DEFAULT false,
                            can_manage boolean DEFAULT false,
                            can_approve boolean DEFAULT false,
                            created_by varchar(100),
                            created_on timestamp,
                            updated_by varchar(100),
                            updated_on timestamp,
                            version bigint NOT NULL DEFAULT 0
                        )', s);
                    EXECUTE format(
                        'CREATE UNIQUE INDEX IF NOT EXISTS uk_responsibility_permission
                         ON %I.responsibility_permissions (organization_id, responsibility_id, menu_id)', s);
                    EXECUTE format(
                        'CREATE INDEX IF NOT EXISTS idx_resp_permission_resp
                         ON %I.responsibility_permissions (responsibility_id)', s);
                    EXECUTE format(
                        'CREATE INDEX IF NOT EXISTS idx_resp_permission_menu
                         ON %I.responsibility_permissions (menu_id)', s);

                    -- Legacy tenant tables required organization_id; JPA admissions
                    -- entities are schema-scoped and do not send that column.
                    IF EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_schema = s AND table_name = 'inquiry' AND column_name = 'organization_id'
                    ) THEN
                        EXECUTE format('ALTER TABLE %I.inquiry ALTER COLUMN organization_id DROP NOT NULL', s);
                    END IF;
                    IF EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_schema = s AND table_name = 'application_admission' AND column_name = 'organization_id'
                    ) THEN
                        EXECUTE format('ALTER TABLE %I.application_admission ALTER COLUMN organization_id DROP NOT NULL', s);
                    END IF;
                END LOOP;
            END $$;
            """;
}
