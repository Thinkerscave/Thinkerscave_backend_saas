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
            applySqlResource("db/migration/V1_33__admissions_crm_production_columns.sql");
            try {
                applySqlResource("db/migration/V1_34__admissions_lead360_alignment.sql");
            } catch (Exception lead360Error) {
                log.warn("Lead360 alignment migration had issues; applying compatibility fallback: {}", lead360Error.getMessage());
            }
            jdbcTemplate.execute(LEAD360_COMPATIBILITY_SQL);
            jdbcTemplate.execute(SESSION_AND_NOTES_SQL);
            jdbcTemplate.execute(LOGIN_HISTORY_RETENTION_SQL);
            log.info("PostgreSQL tenant schema patch applied (admissions CRM + lead360 alignment + user_sessions + counseling_note + login history retention).");
        } catch (Exception ex) {
            log.warn("PostgreSQL tenant schema patch failed: {}", ex.getMessage());
        }
    }

    private void applySqlResource(String classpathFile) throws Exception {
        String sql = StreamUtils.copyToString(new ClassPathResource(classpathFile).getInputStream(), StandardCharsets.UTF_8);
        jdbcTemplate.execute(sql);
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

    private static final String LEAD360_COMPATIBILITY_SQL = """
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
                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = s
                          AND table_name = 'inquiry'
                    ) THEN
                        EXECUTE format('ALTER TABLE %I.inquiry ADD COLUMN IF NOT EXISTS student_name varchar(100)', s);
                        EXECUTE format('ALTER TABLE %I.inquiry ADD COLUMN IF NOT EXISTS parent_contact_name varchar(100)', s);
                        EXECUTE format('UPDATE %I.inquiry SET student_name = COALESCE(NULLIF(TRIM(student_name), ''''), NULLIF(TRIM(name), '''')) WHERE student_name IS NULL OR TRIM(student_name) = ''''', s);
                        EXECUTE format('UPDATE %I.inquiry SET parent_contact_name = COALESCE(NULLIF(TRIM(parent_contact_name), ''''), NULLIF(TRIM(name), '''')) WHERE parent_contact_name IS NULL OR TRIM(parent_contact_name) = ''''', s);
                    END IF;

                    EXECUTE format(
                        'CREATE TABLE IF NOT EXISTS %I.lead_counselor_assignment (
                            assignment_id bigserial PRIMARY KEY,
                            inquiry_id bigint NOT NULL,
                            previous_counselor_staff_id bigint,
                            new_counselor_staff_id bigint NOT NULL,
                            reason varchar(300),
                            assigned_by_user_id bigint,
                            assigned_by_username varchar(100),
                            assigned_on timestamp NOT NULL,
                            active boolean NOT NULL DEFAULT true,
                            created_by varchar(100),
                            created_on timestamp,
                            updated_by varchar(100),
                            updated_on timestamp,
                            version bigint NOT NULL DEFAULT 0
                        )', s);
                END LOOP;
            END $$;
            """;

    private static final String LOGIN_HISTORY_RETENTION_SQL = """
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
                    IF EXISTS (
                        SELECT 1 FROM information_schema.tables
                        WHERE table_schema = s AND table_name = 'login_history'
                    ) THEN
                        EXECUTE format('ALTER TABLE %I.login_history ADD COLUMN IF NOT EXISTS device_name varchar(200)', s);
                        EXECUTE format('ALTER TABLE %I.login_history ADD COLUMN IF NOT EXISTS logout_ip_address varchar(100)', s);
                        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_login_history_login_time ON %I.login_history (login_time)', s);
                    END IF;
                END LOOP;

                CREATE TABLE IF NOT EXISTS public.retention_purge_log (
                    id bigserial PRIMARY KEY,
                    task_key varchar(64) NOT NULL,
                    retention_days int NOT NULL,
                    cutoff_at timestamp NOT NULL,
                    deleted_count int NOT NULL DEFAULT 0,
                    trigger_type varchar(32) NOT NULL,
                    organization_id bigint,
                    actor_username varchar(100),
                    summary varchar(500),
                    ran_at timestamp NOT NULL,
                    created_by varchar(100),
                    created_on timestamp,
                    updated_by varchar(100),
                    updated_on timestamp,
                    version bigint NOT NULL DEFAULT 0
                );
                CREATE INDEX IF NOT EXISTS idx_retention_purge_task_ran
                    ON public.retention_purge_log (task_key, ran_at);
            END $$;
            """;
}
