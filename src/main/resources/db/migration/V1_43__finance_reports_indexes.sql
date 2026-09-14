-- Finance Reports V1 aggregate-query indexes (public + tenant_*).
DO $$
DECLARE
    s text;
BEGIN
    FOR s IN
        SELECT nspname
          FROM pg_namespace
         WHERE nspname = 'public' OR nspname LIKE 'tenant_%'
    LOOP
        IF to_regclass(format('%I.payroll_payment', s)) IS NOT NULL THEN
            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_payroll_payment_paid_on ON %I.payroll_payment (paid_on)', s);
        END IF;
        IF to_regclass(format('%I.expense_payment', s)) IS NOT NULL THEN
            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_payment_paid_on ON %I.expense_payment (paid_on)', s);
        END IF;
    END LOOP;
END $$;
