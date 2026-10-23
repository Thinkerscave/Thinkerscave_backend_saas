-- V29: Drop legacy staff payroll tables. Their rows were copied to Finance Payroll (V28) and no code reads them.

DROP TABLE IF EXISTS staff_salary_structure;
DROP TABLE IF EXISTS payroll;
