-- ============================================================================
-- Data Migration: Populate user_tenant_mapping from existing tenants
-- ============================================================================

-- Populate user_tenant_mapping with existing users from all schemas
-- This script finds all non-system schemas and extracts users from each

DO $$
DECLARE
    schema_rec RECORD;
    user_rec RECORD;
    inserted_count INTEGER := 0;
BEGIN
    -- Loop through all schemas except system ones
    FOR schema_rec IN 
        SELECT schema_name 
        FROM information_schema.schemata 
        WHERE schema_name NOT IN ('public', 'information_schema', 'pg_catalog', 'pg_toast')
          AND schema_name NOT LIKE 'pg_%'
        ORDER BY schema_name
    LOOP
        RAISE NOTICE 'Processing schema: %', schema_rec.schema_name;
        
        -- Check if users table exists in this schema
        IF EXISTS (
            SELECT 1 
            FROM information_schema.tables 
            WHERE table_schema = schema_rec.schema_name 
            AND table_name = 'users'
        ) THEN
            -- Insert users from this schema into the mapping
            FOR user_rec IN EXECUTE format(
                'SELECT email, user_name FROM %I.users WHERE email IS NOT NULL AND user_name IS NOT NULL',
                schema_rec.schema_name
            )
            LOOP
                BEGIN
                    INSERT INTO public.user_tenant_mapping (email, username, tenant_id)
                    VALUES (user_rec.email, user_rec.user_name, schema_rec.schema_name)
                    ON CONFLICT (email) DO NOTHING;
                    
                    inserted_count := inserted_count + 1;
                EXCEPTION
                    WHEN OTHERS THEN
                        RAISE WARNING 'Failed to insert user % from schema %: %', 
                                     user_rec.email, schema_rec.schema_name, SQLERRM;
                END;
            END LOOP;
        ELSE
            RAISE NOTICE 'Schema % does not have a users table, skipping', schema_rec.schema_name;
        END IF;
    END LOOP;
    
    RAISE NOTICE 'Migration complete. Inserted % users into mapping table', inserted_count;
END $$;

-- Verify the results
SELECT 
    tenant_id,
    COUNT(*) as user_count
FROM public.user_tenant_mapping
GROUP BY tenant_id
ORDER BY tenant_id;
