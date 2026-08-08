-- V1_27: Payroll Phase-1 deductions + payslip snapshot columns (schema-per-tenant).

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
            WHERE table_schema = s AND table_name = 'staff_salary_structure'
        ) THEN
            EXECUTE format('ALTER TABLE %I.staff_salary_structure ADD COLUMN IF NOT EXISTS pf_employee numeric(12,2) DEFAULT 0', s);
            EXECUTE format('ALTER TABLE %I.staff_salary_structure ADD COLUMN IF NOT EXISTS esi_employee numeric(12,2) DEFAULT 0', s);
            EXECUTE format('ALTER TABLE %I.staff_salary_structure ADD COLUMN IF NOT EXISTS professional_tax numeric(12,2) DEFAULT 0', s);
            EXECUTE format('ALTER TABLE %I.staff_salary_structure ADD COLUMN IF NOT EXISTS other_deduction numeric(12,2) DEFAULT 0', s);
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = s AND table_name = 'payroll'
        ) THEN
            EXECUTE format('ALTER TABLE %I.payroll ADD COLUMN IF NOT EXISTS pf_amount numeric(12,2) DEFAULT 0', s);
            EXECUTE format('ALTER TABLE %I.payroll ADD COLUMN IF NOT EXISTS esi_amount numeric(12,2) DEFAULT 0', s);
            EXECUTE format('ALTER TABLE %I.payroll ADD COLUMN IF NOT EXISTS professional_tax_amount numeric(12,2) DEFAULT 0', s);
            EXECUTE format('ALTER TABLE %I.payroll ADD COLUMN IF NOT EXISTS other_deduction_amount numeric(12,2) DEFAULT 0', s);
        END IF;
    END LOOP;
END $$;
