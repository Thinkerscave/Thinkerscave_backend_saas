-- V28: One-way copy of legacy staff salary/payroll rows into Finance Payroll tables.
--  * Idempotent: rows already represented in Finance are skipped.
--  * Tenants without the legacy or finance tables are skipped.
--  * Legacy tables `payroll` and `staff_salary_structure` are kept (not dropped).

DO $$
BEGIN
IF to_regclass('staff_salary_structure') IS NULL OR to_regclass('payroll') IS NULL
   OR to_regclass('salary_component') IS NULL OR to_regclass('employee_payroll') IS NULL THEN
    RETURN;
END IF;

INSERT INTO salary_component (code, name, component_type, calculation_method, default_value, statutory_code, status, sort_order, version)
SELECT v.code, v.name, v.ctype, v.calc, v.dval, v.stat, 'ACTIVE', v.sort_order, 0
FROM (VALUES
    ('BASIC', 'Basic Pay', 'EARNING', 'FIXED_AMOUNT', NULL::numeric, NULL::varchar, 10),
    ('HRA', 'HRA', 'EARNING', 'FIXED_AMOUNT', NULL, NULL, 20),
    ('DA', 'Dearness Allowance', 'EARNING', 'FIXED_AMOUNT', NULL, NULL, 30),
    ('SPECIAL', 'Special Allowance', 'EARNING', 'FIXED_AMOUNT', NULL, NULL, 40),
    ('TRANSPORT', 'Transport Allowance', 'EARNING', 'FIXED_AMOUNT', NULL, NULL, 50),
    ('OTHER_EARNING', 'Other Allowance', 'EARNING', 'FIXED_AMOUNT', NULL, NULL, 60),
    ('PF', 'Provident Fund', 'DEDUCTION', 'FIXED_AMOUNT', NULL, 'PF', 100),
    ('ESI', 'ESI', 'DEDUCTION', 'FIXED_AMOUNT', NULL, 'ESI', 110),
    ('PT', 'Professional Tax', 'DEDUCTION', 'FIXED_AMOUNT', NULL, 'PT', 120),
    ('TDS', 'TDS', 'DEDUCTION', 'FIXED_AMOUNT', NULL, 'TDS', 130),
    ('OTHER_DED', 'Other Deduction', 'DEDUCTION', 'FIXED_AMOUNT', NULL, NULL, 140),
    ('PF_EMPLOYER', 'Employer PF', 'EMPLOYER_CONTRIBUTION', 'FIXED_AMOUNT', NULL, 'PF', 200),
    ('ESI_EMPLOYER', 'Employer ESI', 'EMPLOYER_CONTRIBUTION', 'FIXED_AMOUNT', NULL, 'ESI', 210)
) AS v(code, name, ctype, calc, dval, stat, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM salary_component c WHERE c.code = v.code);

INSERT INTO employee_salary (
    staff_id, payment_type, salary_structure_id, effective_from, effective_to, active,
    bank_name, account_holder_name, account_number, ifsc_code, remarks, version)
SELECT sss.staff_id, 'SALARY', NULL,
       COALESCE(sss.effective_from, CURRENT_DATE), sss.effective_to, COALESCE(sss.active, true),
       sss.bank_name, sss.account_holder_name, sss.account_number, sss.ifsc_code, sss.remarks, 0
FROM staff_salary_structure sss
WHERE NOT EXISTS (
    SELECT 1 FROM employee_salary es
    WHERE es.staff_id = sss.staff_id
      AND es.effective_from = COALESCE(sss.effective_from, CURRENT_DATE)
      AND COALESCE(es.effective_to, DATE '9999-12-31') = COALESCE(sss.effective_to, DATE '9999-12-31'));

INSERT INTO employee_salary_component (
    employee_salary_id, salary_component_id, component_type, calculation_method, value, applicable, version)
SELECT es.employee_salary_id, sc.salary_component_id, sc.component_type, 'FIXED_AMOUNT', amt.val, true, 0
FROM staff_salary_structure sss
JOIN employee_salary es
  ON es.staff_id = sss.staff_id
 AND es.effective_from = COALESCE(sss.effective_from, CURRENT_DATE)
 AND COALESCE(es.effective_to, DATE '9999-12-31') = COALESCE(sss.effective_to, DATE '9999-12-31')
CROSS JOIN LATERAL (VALUES
    ('BASIC', COALESCE(sss.basic_pay, 0)),
    ('HRA', COALESCE(sss.hra, 0)),
    ('DA', COALESCE(sss.da, 0)),
    ('SPECIAL', COALESCE(sss.special_allowance, 0)),
    ('TRANSPORT', COALESCE(sss.transport_allowance, 0)),
    ('OTHER_EARNING', COALESCE(sss.other_allowance, 0)),
    ('PF', COALESCE(sss.pf_employee, 0)),
    ('ESI', COALESCE(sss.esi_employee, 0)),
    ('PT', COALESCE(sss.professional_tax, 0)),
    ('OTHER_DED', COALESCE(sss.other_deduction, 0))
) AS amt(code, val)
JOIN salary_component sc ON sc.code = amt.code
WHERE amt.val <> 0
  AND NOT EXISTS (
      SELECT 1 FROM employee_salary_component esc
      WHERE esc.employee_salary_id = es.employee_salary_id
        AND esc.salary_component_id = sc.salary_component_id);

INSERT INTO payroll_run (payroll_year, payroll_month, status, generated_on, version)
SELECT p.payroll_year, p.payroll_month,
       CASE WHEN bool_and(p.status = 'PAID') THEN 'PAID'
            WHEN bool_or(p.status = 'PAID') THEN 'PARTIALLY_PAID'
            ELSE 'GENERATED' END,
       MIN(p.generated_on)::timestamptz, 0
FROM payroll p
WHERE NOT EXISTS (
    SELECT 1 FROM payroll_run r
    WHERE r.payroll_year = p.payroll_year AND r.payroll_month = p.payroll_month)
GROUP BY p.payroll_year, p.payroll_month;

INSERT INTO employee_payroll (
    payroll_run_id, staff_id, payroll_year, payroll_month, payment_type,
    working_days, present_days, lop_days, gross_amount, total_deductions, net_amount, status, version)
SELECT r.payroll_run_id, p.staff_id, p.payroll_year, p.payroll_month, 'SALARY',
       p.working_days, p.present_days, p.leave_without_pay_days,
       COALESCE(p.gross_salary, 0), COALESCE(p.total_deductions, 0), COALESCE(p.net_salary, 0),
       CASE WHEN p.status = 'PAID' THEN 'PAID' ELSE 'GENERATED' END, 0
FROM payroll p
JOIN payroll_run r ON r.payroll_year = p.payroll_year AND r.payroll_month = p.payroll_month
WHERE NOT EXISTS (
    SELECT 1 FROM employee_payroll ep
    WHERE ep.staff_id = p.staff_id AND ep.payroll_year = p.payroll_year AND ep.payroll_month = p.payroll_month);

INSERT INTO employee_payroll_line (
    employee_payroll_id, salary_component_id, component_code, component_name,
    component_type, calculation_method, rate_or_percent, amount, sort_order, version)
SELECT ep.employee_payroll_id, sc.salary_component_id, sc.code, sc.name,
       sc.component_type, 'FIXED_AMOUNT', NULL, amt.val, sc.sort_order, 0
FROM payroll p
JOIN employee_payroll ep
  ON ep.staff_id = p.staff_id AND ep.payroll_year = p.payroll_year AND ep.payroll_month = p.payroll_month
CROSS JOIN LATERAL (VALUES
    ('BASIC', COALESCE(p.gross_salary, 0)),
    ('PF', COALESCE(p.pf_amount, 0)),
    ('ESI', COALESCE(p.esi_amount, 0)),
    ('PT', COALESCE(p.professional_tax_amount, 0)),
    ('OTHER_DED', COALESCE(p.other_deduction_amount, 0))
) AS amt(code, val)
JOIN salary_component sc ON sc.code = amt.code
WHERE amt.val <> 0
  AND NOT EXISTS (
      SELECT 1 FROM employee_payroll_line epl
      WHERE epl.employee_payroll_id = ep.employee_payroll_id AND epl.component_code = sc.code);
END $$;
