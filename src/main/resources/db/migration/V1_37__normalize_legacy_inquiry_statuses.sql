-- Normalize legacy inquiry / follow-up status values that predate Lead 360.
-- V1_34 cleaned inquiry.status but not inquiry_follow_up.status_after
-- (causes: No enum constant InquiryStatus.MEETING_SCHEDULED on /follow-ups/overdue).
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
            WHERE table_schema = s AND table_name = 'inquiry'
        ) THEN
            EXECUTE format($f$
                UPDATE %I.inquiry
                SET status = CASE UPPER(REPLACE(REPLACE(COALESCE(status, 'NEW'), ' ', '_'), '-', '_'))
                    WHEN 'FOLLOW_UP' THEN 'CONTACTED'
                    WHEN 'MEETING_SCHEDULED' THEN 'CONTACTED'
                    WHEN 'FOLLOW_UP_REQUIRED' THEN 'CONTACTED'
                    WHEN 'COUNSELING' THEN 'INTERESTED'
                    WHEN 'DOCUMENTS_PENDING' THEN 'INTERESTED'
                    WHEN 'READY_FOR_ADMISSION' THEN 'INTERESTED'
                    WHEN 'CONVERTED' THEN 'APPLICATION_SUBMITTED'
                    WHEN 'CLOSED' THEN 'LOST'
                    ELSE UPPER(REPLACE(REPLACE(COALESCE(status, 'NEW'), ' ', '_'), '-', '_'))
                END
            $f$, s);
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = s
              AND table_name = 'inquiry_follow_up'
              AND column_name = 'status_after'
        ) THEN
            EXECUTE format($f$
                UPDATE %I.inquiry_follow_up
                SET status_after = CASE UPPER(REPLACE(REPLACE(status_after, ' ', '_'), '-', '_'))
                    WHEN 'FOLLOW_UP' THEN 'CONTACTED'
                    WHEN 'MEETING_SCHEDULED' THEN 'CONTACTED'
                    WHEN 'FOLLOW_UP_REQUIRED' THEN 'CONTACTED'
                    WHEN 'COUNSELING' THEN 'INTERESTED'
                    WHEN 'DOCUMENTS_PENDING' THEN 'INTERESTED'
                    WHEN 'READY_FOR_ADMISSION' THEN 'INTERESTED'
                    WHEN 'CONVERTED' THEN 'APPLICATION_SUBMITTED'
                    WHEN 'CLOSED' THEN 'LOST'
                    ELSE UPPER(REPLACE(REPLACE(status_after, ' ', '_'), '-', '_'))
                END
                WHERE status_after IS NOT NULL
            $f$, s);
        END IF;
    END LOOP;
END $$;
