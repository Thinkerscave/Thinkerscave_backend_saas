-- Hibernate ddl-auto=update only alters the current (usually public) schema.
-- Tenant copies then miss newer columns and login/sidebar fail with:
--   ERROR: column "..." does not exist
-- Copy any public column that is missing from a matching tenant_% table.

DO $$
DECLARE
    rec RECORD;
    ddl text;
BEGIN
    FOR rec IN
        SELECT
            tn.nspname AS tenant_schema,
            pc.relname AS table_name,
            pa.attname AS column_name,
            pg_catalog.format_type(pa.atttypid, pa.atttypmod) AS col_type,
            pg_get_expr(ad.adbin, ad.adrelid) AS col_default
        FROM pg_attribute pa
        JOIN pg_class pc ON pc.oid = pa.attrelid AND pc.relkind = 'r'
        JOIN pg_namespace pn ON pn.oid = pc.relnamespace AND pn.nspname = 'public'
        JOIN pg_namespace tn ON tn.nspname LIKE 'tenant_%'
        JOIN pg_class tc ON tc.relnamespace = tn.oid AND tc.relname = pc.relname AND tc.relkind = 'r'
        LEFT JOIN pg_attrdef ad ON ad.adrelid = pa.attrelid AND ad.adnum = pa.attnum
        WHERE pa.attnum > 0
          AND NOT pa.attisdropped
          AND NOT EXISTS (
              SELECT 1
              FROM pg_attribute ta
              WHERE ta.attrelid = tc.oid
                AND ta.attname = pa.attname
                AND ta.attnum > 0
                AND NOT ta.attisdropped
          )
        ORDER BY tn.nspname, pc.relname, pa.attnum
    LOOP
        ddl := format(
            'ALTER TABLE %I.%I ADD COLUMN IF NOT EXISTS %I %s',
            rec.tenant_schema, rec.table_name, rec.column_name, rec.col_type
        );
        IF rec.col_default IS NOT NULL AND rec.col_default NOT ILIKE '%nextval%' THEN
            ddl := ddl || ' DEFAULT ' || rec.col_default;
        END IF;
        EXECUTE ddl;
        RAISE NOTICE 'Added %.%.%', rec.tenant_schema, rec.table_name, rec.column_name;
    END LOOP;
END $$;
