-- V1_28: Rebuild Academics module schema per frozen baseline design.
-- Soft-delete uses is_active (false keeps the row; updated_on is refreshed).
-- Audit columns (created_by/on, updated_by/on, version) are on every table.
-- Applies to public and tenant_* schemas. Destructive: drops legacy Academics tables and data.
-- Flyway may be disabled in some environments; safe to apply manually per tenant schema.

DO $$
DECLARE
    s text;
    r record;
BEGIN
    FOR s IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname = 'public'
           OR nspname LIKE 'tenant_%'
    LOOP
        -- Drop inbound FK constraints from other modules referencing legacy Academics tables.
        FOR r IN
            SELECT tc.table_name, tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
              ON tc.constraint_name = ccu.constraint_name
             AND tc.table_schema = ccu.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
              AND tc.table_schema = s
              AND ccu.table_name IN (
                  'academic_year', 'academic_class', 'academic_section', 'subject',
                  'section', 'class', 'academic_years'
              )
        LOOP
            EXECUTE format(
                'ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
                s, r.table_name, r.constraint_name
            );
        END LOOP;

        -- Drop legacy and rebuilt Academics tables (children first).
        EXECUTE format('DROP TABLE IF EXISTS %I.syllabus_coverage CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.syllabus_topic CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.syllabus_chapter CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.syllabus_unit CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.syllabus CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.teacher_arrangement CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.timetable_slot CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.subject_assignment CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.timetable_template CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.period_template CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.class_schedule_assignment CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.academic_schedule CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.academic_calendar_event CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.academic_setting CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.academic_setup_progress CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.timetable_conflict CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.timetable_entry CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.timetable_version CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.timetable_period CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.timetable_working_day CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.timetable_configuration CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.teacher_allocation_teacher CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.teacher_allocation CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.class_teacher_assignment CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.class_subject_mapping CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.academic_year_transition CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.academic_section CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.section CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.subject CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.academic_class CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.class CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.academic_resource CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.academic_year CASCADE', s);
        EXECUTE format('DROP TABLE IF EXISTS %I.academic_years CASCADE', s);

        -- ------------------------------------------------------------------
        -- academic_year
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.academic_year (
                academic_year_id BIGSERIAL PRIMARY KEY,
                name VARCHAR(50) NOT NULL,
                start_date DATE NOT NULL,
                end_date DATE NOT NULL,
                pattern VARCHAR(30) NOT NULL DEFAULT 'ANNUAL',
                status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
                submitted_at TIMESTAMP,
                submitted_by_user_id BIGINT,
                approved_at TIMESTAMP,
                approved_by_user_id BIGINT,
                rejected_at TIMESTAMP,
                rejected_by_user_id BIGINT,
                rejection_reason VARCHAR(1000),
                activated_at TIMESTAMP,
                activated_by_user_id BIGINT,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT chk_academic_year_dates CHECK (end_date > start_date)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE UNIQUE INDEX uk_academic_year_name ON %I.academic_year (name) WHERE is_active = TRUE',
            s
        );
        EXECUTE format(
            'CREATE UNIQUE INDEX uk_academic_year_current ON %I.academic_year ((1)) WHERE status = ''CURRENT'' AND is_active = TRUE',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_academic_year_status ON %I.academic_year (status)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_academic_year_dates ON %I.academic_year (start_date, end_date)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_academic_year_active ON %I.academic_year (is_active)',
            s
        );

        -- ------------------------------------------------------------------
        -- academic_class
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.academic_class (
                class_id BIGSERIAL PRIMARY KEY,
                academic_year_id BIGINT NOT NULL REFERENCES %1$I.academic_year(academic_year_id),
                name VARCHAR(100) NOT NULL,
                code VARCHAR(50) NOT NULL,
                stage VARCHAR(40) NOT NULL,
                display_order INTEGER NOT NULL DEFAULT 0,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT chk_academic_class_display_order CHECK (display_order >= 0)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE UNIQUE INDEX uk_academic_class_year_code ON %I.academic_class (academic_year_id, code) WHERE is_active = TRUE',
            s
        );
        EXECUTE format(
            'CREATE UNIQUE INDEX uk_academic_class_id_year ON %I.academic_class (class_id, academic_year_id)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_academic_class_year_active ON %I.academic_class (academic_year_id, is_active)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_academic_class_stage ON %I.academic_class (stage)',
            s
        );

        -- ------------------------------------------------------------------
        -- academic_resource (before section.default_resource_id FK)
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.academic_resource (
                academic_resource_id BIGSERIAL PRIMARY KEY,
                name VARCHAR(150) NOT NULL,
                code VARCHAR(50) NOT NULL,
                resource_type VARCHAR(30) NOT NULL,
                capacity INTEGER,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT chk_academic_resource_capacity CHECK (capacity IS NULL OR capacity > 0)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE UNIQUE INDEX uk_academic_resource_code ON %I.academic_resource (code) WHERE is_active = TRUE',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_academic_resource_type ON %I.academic_resource (resource_type)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_academic_resource_active ON %I.academic_resource (is_active)',
            s
        );

        -- ------------------------------------------------------------------
        -- academic_section
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.academic_section (
                section_id BIGSERIAL PRIMARY KEY,
                class_id BIGINT NOT NULL REFERENCES %1$I.academic_class(class_id),
                name VARCHAR(50) NOT NULL,
                code VARCHAR(50) NOT NULL,
                capacity INTEGER,
                default_resource_id BIGINT REFERENCES %1$I.academic_resource(academic_resource_id),
                display_order INTEGER NOT NULL DEFAULT 0,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT chk_academic_section_capacity CHECK (capacity IS NULL OR capacity > 0),
                CONSTRAINT chk_academic_section_display_order CHECK (display_order >= 0)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE UNIQUE INDEX uk_academic_section_class_code ON %I.academic_section (class_id, code) WHERE is_active = TRUE',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_academic_section_class_active ON %I.academic_section (class_id, is_active)',
            s
        );

        -- ------------------------------------------------------------------
        -- subject
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.subject (
                subject_id BIGSERIAL PRIMARY KEY,
                academic_year_id BIGINT NOT NULL REFERENCES %1$I.academic_year(academic_year_id),
                name VARCHAR(150) NOT NULL,
                code VARCHAR(50) NOT NULL,
                category VARCHAR(30) NOT NULL,
                default_weekly_periods SMALLINT NOT NULL,
                timetable_preference VARCHAR(30) NOT NULL DEFAULT 'ANY',
                description VARCHAR(500),
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT chk_subject_weekly_periods CHECK (default_weekly_periods > 0)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE UNIQUE INDEX uk_subject_year_code ON %I.subject (academic_year_id, code) WHERE is_active = TRUE',
            s
        );
        EXECUTE format(
            'CREATE UNIQUE INDEX uk_subject_id_year ON %I.subject (subject_id, academic_year_id)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_subject_year_active ON %I.subject (academic_year_id, is_active)',
            s
        );

        -- ------------------------------------------------------------------
        -- class_subject_mapping
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.class_subject_mapping (
                class_subject_mapping_id BIGSERIAL PRIMARY KEY,
                academic_class_id BIGINT NOT NULL REFERENCES %1$I.academic_class(class_id),
                subject_id BIGINT NOT NULL REFERENCES %1$I.subject(subject_id),
                weekly_periods SMALLINT NOT NULL,
                timetable_preference VARCHAR(30) NOT NULL DEFAULT 'ANY',
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT chk_csm_weekly_periods CHECK (weekly_periods > 0)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE UNIQUE INDEX uk_csm_class_subject ON %I.class_subject_mapping (academic_class_id, subject_id) WHERE is_active = TRUE',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_csm_class_active ON %I.class_subject_mapping (academic_class_id, is_active)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_csm_subject ON %I.class_subject_mapping (subject_id)',
            s
        );

        -- ------------------------------------------------------------------
        -- teacher_allocation
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.teacher_allocation (
                teacher_allocation_id BIGSERIAL PRIMARY KEY,
                section_id BIGINT NOT NULL REFERENCES %1$I.academic_section(section_id),
                class_subject_mapping_id BIGINT NOT NULL REFERENCES %1$I.class_subject_mapping(class_subject_mapping_id),
                status VARCHAR(30) NOT NULL DEFAULT 'UNASSIGNED',
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0
            )
        $sql$, s);

        EXECUTE format(
            'CREATE UNIQUE INDEX uk_teacher_allocation_section_mapping ON %I.teacher_allocation (section_id, class_subject_mapping_id) WHERE is_active = TRUE',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_teacher_allocation_section_status ON %I.teacher_allocation (section_id, status)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_teacher_allocation_mapping_status ON %I.teacher_allocation (class_subject_mapping_id, status)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_teacher_allocation_active ON %I.teacher_allocation (is_active)',
            s
        );

        -- ------------------------------------------------------------------
        -- teacher_allocation_teacher
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.teacher_allocation_teacher (
                teacher_allocation_teacher_id BIGSERIAL PRIMARY KEY,
                teacher_allocation_id BIGINT NOT NULL REFERENCES %1$I.teacher_allocation(teacher_allocation_id),
                staff_id BIGINT NOT NULL,
                role VARCHAR(20) NOT NULL DEFAULT 'SECONDARY',
                effective_from DATE NOT NULL,
                effective_to DATE,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT chk_tat_effective_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE INDEX idx_tat_staff_effective ON %I.teacher_allocation_teacher (staff_id, effective_from)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_tat_allocation_effective ON %I.teacher_allocation_teacher (teacher_allocation_id, effective_from)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_tat_active ON %I.teacher_allocation_teacher (is_active)',
            s
        );

        -- ------------------------------------------------------------------
        -- class_teacher_assignment
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.class_teacher_assignment (
                class_teacher_assignment_id BIGSERIAL PRIMARY KEY,
                section_id BIGINT NOT NULL REFERENCES %1$I.academic_section(section_id),
                staff_id BIGINT NOT NULL,
                effective_from DATE NOT NULL,
                effective_to DATE,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT chk_cta_effective_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE INDEX idx_cta_section_effective ON %I.class_teacher_assignment (section_id, effective_from)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_cta_staff_effective ON %I.class_teacher_assignment (staff_id, effective_from)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_cta_active ON %I.class_teacher_assignment (is_active)',
            s
        );

        -- ------------------------------------------------------------------
        -- timetable_configuration
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.timetable_configuration (
                timetable_configuration_id BIGSERIAL PRIMARY KEY,
                academic_year_id BIGINT NOT NULL REFERENCES %1$I.academic_year(academic_year_id),
                name VARCHAR(100) NOT NULL,
                shift_type VARCHAR(30) NOT NULL,
                school_start_time TIME NOT NULL,
                school_end_time TIME NOT NULL,
                default_period_duration_min SMALLINT,
                max_teacher_weekly_periods SMALLINT NOT NULL,
                status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                is_locked BOOLEAN NOT NULL DEFAULT FALSE,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT chk_timetable_config_times CHECK (school_end_time > school_start_time),
                CONSTRAINT chk_timetable_config_max_periods CHECK (max_teacher_weekly_periods > 0)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE UNIQUE INDEX uk_timetable_config_year_shift ON %I.timetable_configuration (academic_year_id, shift_type) WHERE is_active = TRUE',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_timetable_config_year_status ON %I.timetable_configuration (academic_year_id, status)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_timetable_config_active ON %I.timetable_configuration (is_active)',
            s
        );

        -- ------------------------------------------------------------------
        -- timetable_working_day
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.timetable_working_day (
                timetable_working_day_id BIGSERIAL PRIMARY KEY,
                timetable_configuration_id BIGINT NOT NULL REFERENCES %1$I.timetable_configuration(timetable_configuration_id),
                day_of_week VARCHAR(15) NOT NULL,
                is_working BOOLEAN NOT NULL DEFAULT TRUE,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT uk_timetable_working_day_config_day UNIQUE (timetable_configuration_id, day_of_week)
            )
        $sql$, s);

        -- ------------------------------------------------------------------
        -- timetable_period
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.timetable_period (
                timetable_period_id BIGSERIAL PRIMARY KEY,
                timetable_configuration_id BIGINT NOT NULL REFERENCES %1$I.timetable_configuration(timetable_configuration_id),
                period_number SMALLINT NOT NULL,
                name VARCHAR(50) NOT NULL,
                start_time TIME NOT NULL,
                end_time TIME NOT NULL,
                slot_kind VARCHAR(20) NOT NULL,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT uk_timetable_period_config_number UNIQUE (timetable_configuration_id, period_number),
                CONSTRAINT chk_timetable_period_times CHECK (end_time > start_time)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE INDEX idx_timetable_period_config_start ON %I.timetable_period (timetable_configuration_id, start_time)',
            s
        );

        -- ------------------------------------------------------------------
        -- timetable_version
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.timetable_version (
                timetable_version_id BIGSERIAL PRIMARY KEY,
                academic_year_id BIGINT NOT NULL REFERENCES %1$I.academic_year(academic_year_id),
                timetable_configuration_id BIGINT NOT NULL REFERENCES %1$I.timetable_configuration(timetable_configuration_id),
                version_number INTEGER NOT NULL,
                generation_status VARCHAR(40) NOT NULL DEFAULT 'NOT_GENERATED',
                status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
                generated_at TIMESTAMP,
                approved_at TIMESTAMP,
                approved_by_user_id BIGINT,
                published_at TIMESTAMP,
                published_by_user_id BIGINT,
                superseded_at TIMESTAMP,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT uk_timetable_version_year_number UNIQUE (academic_year_id, version_number)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE INDEX idx_timetable_version_year_status ON %I.timetable_version (academic_year_id, status)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_timetable_version_config ON %I.timetable_version (timetable_configuration_id)',
            s
        );
        EXECUTE format(
            'CREATE UNIQUE INDEX uk_timetable_version_published ON %I.timetable_version (academic_year_id, timetable_configuration_id) WHERE status = ''PUBLISHED''',
            s
        );

        -- ------------------------------------------------------------------
        -- timetable_entry
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.timetable_entry (
                timetable_entry_id BIGSERIAL PRIMARY KEY,
                timetable_version_id BIGINT NOT NULL REFERENCES %1$I.timetable_version(timetable_version_id),
                day_of_week VARCHAR(15) NOT NULL,
                timetable_period_id BIGINT NOT NULL REFERENCES %1$I.timetable_period(timetable_period_id),
                section_id BIGINT REFERENCES %1$I.academic_section(section_id),
                teacher_allocation_id BIGINT REFERENCES %1$I.teacher_allocation(teacher_allocation_id),
                resource_id BIGINT REFERENCES %1$I.academic_resource(academic_resource_id),
                entry_type VARCHAR(20) NOT NULL,
                subject_name_snapshot VARCHAR(150),
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT uk_timetable_entry_slot UNIQUE (timetable_version_id, section_id, day_of_week, timetable_period_id),
                CONSTRAINT chk_timetable_entry_subject CHECK (
                    entry_type <> 'SUBJECT'
                    OR (section_id IS NOT NULL AND teacher_allocation_id IS NOT NULL)
                ),
                CONSTRAINT chk_timetable_entry_free_activity CHECK (
                    entry_type NOT IN ('FREE_PERIOD', 'ACTIVITY')
                    OR teacher_allocation_id IS NULL
                )
            )
        $sql$, s);

        EXECUTE format(
            'CREATE INDEX idx_timetable_entry_version_day_period ON %I.timetable_entry (timetable_version_id, day_of_week, timetable_period_id)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_timetable_entry_allocation ON %I.timetable_entry (teacher_allocation_id)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_timetable_entry_resource ON %I.timetable_entry (resource_id)',
            s
        );

        -- ------------------------------------------------------------------
        -- timetable_conflict
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.timetable_conflict (
                timetable_conflict_id BIGSERIAL PRIMARY KEY,
                timetable_version_id BIGINT NOT NULL REFERENCES %1$I.timetable_version(timetable_version_id),
                conflict_type VARCHAR(50) NOT NULL,
                is_blocking BOOLEAN NOT NULL DEFAULT TRUE,
                status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                message VARCHAR(1000) NOT NULL,
                timetable_entry_id BIGINT REFERENCES %1$I.timetable_entry(timetable_entry_id),
                related_timetable_entry_id BIGINT REFERENCES %1$I.timetable_entry(timetable_entry_id),
                section_id BIGINT REFERENCES %1$I.academic_section(section_id),
                teacher_allocation_id BIGINT REFERENCES %1$I.teacher_allocation(teacher_allocation_id),
                resource_id BIGINT REFERENCES %1$I.academic_resource(academic_resource_id),
                day_of_week VARCHAR(15),
                timetable_period_id BIGINT REFERENCES %1$I.timetable_period(timetable_period_id),
                resolved_at TIMESTAMP,
                resolved_by_user_id BIGINT,
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0
            )
        $sql$, s);

        EXECUTE format(
            'CREATE INDEX idx_timetable_conflict_version_status ON %I.timetable_conflict (timetable_version_id, status)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_timetable_conflict_blocking ON %I.timetable_conflict (timetable_version_id, is_blocking)',
            s
        );
        EXECUTE format(
            'CREATE INDEX idx_timetable_conflict_allocation ON %I.timetable_conflict (teacher_allocation_id)',
            s
        );

        -- ------------------------------------------------------------------
        -- academic_year_transition
        -- ------------------------------------------------------------------
        EXECUTE format($sql$
            CREATE TABLE %1$I.academic_year_transition (
                academic_year_transition_id BIGSERIAL PRIMARY KEY,
                source_academic_year_id BIGINT NOT NULL REFERENCES %1$I.academic_year(academic_year_id),
                target_academic_year_id BIGINT NOT NULL REFERENCES %1$I.academic_year(academic_year_id),
                status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED',
                copy_classes BOOLEAN NOT NULL DEFAULT FALSE,
                copy_sections BOOLEAN NOT NULL DEFAULT FALSE,
                copy_subjects BOOLEAN NOT NULL DEFAULT FALSE,
                copy_mappings BOOLEAN NOT NULL DEFAULT FALSE,
                copy_allocations BOOLEAN NOT NULL DEFAULT FALSE,
                started_at TIMESTAMP,
                completed_at TIMESTAMP,
                approved_at TIMESTAMP,
                approved_by_user_id BIGINT,
                activated_at TIMESTAMP,
                activated_by_user_id BIGINT,
                failure_reason VARCHAR(1000),
                created_by VARCHAR(100),
                created_on TIMESTAMP,
                updated_by VARCHAR(100),
                updated_on TIMESTAMP,
                version BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT uk_academic_year_transition_pair UNIQUE (source_academic_year_id, target_academic_year_id),
                CONSTRAINT chk_academic_year_transition_distinct CHECK (source_academic_year_id <> target_academic_year_id)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE INDEX idx_academic_year_transition_target_status ON %I.academic_year_transition (target_academic_year_id, status)',
            s
        );

        -- Re-add FK constraints for Student module (when tables exist).
        IF to_regclass(format('%I.student_enrollment', s)) IS NOT NULL THEN
            EXECUTE format(
                'ALTER TABLE %I.student_enrollment ADD CONSTRAINT fk_enrollment_academic_year FOREIGN KEY (academic_year_id) REFERENCES %I.academic_year(academic_year_id)',
                s, s
            );
            EXECUTE format(
                'ALTER TABLE %I.student_enrollment ADD CONSTRAINT fk_enrollment_class FOREIGN KEY (class_id) REFERENCES %I.academic_class(class_id)',
                s, s
            );
            EXECUTE format(
                'ALTER TABLE %I.student_enrollment ADD CONSTRAINT fk_enrollment_section FOREIGN KEY (section_id) REFERENCES %I.academic_section(section_id)',
                s, s
            );
        END IF;

        IF to_regclass(format('%I.promotion_batch', s)) IS NOT NULL THEN
            EXECUTE format(
                'ALTER TABLE %I.promotion_batch ADD CONSTRAINT fk_promotion_batch_from_year FOREIGN KEY (from_academic_year_id) REFERENCES %I.academic_year(academic_year_id)',
                s, s
            );
            EXECUTE format(
                'ALTER TABLE %I.promotion_batch ADD CONSTRAINT fk_promotion_batch_to_year FOREIGN KEY (to_academic_year_id) REFERENCES %I.academic_year(academic_year_id)',
                s, s
            );
        END IF;

    END LOOP;
END $$;
