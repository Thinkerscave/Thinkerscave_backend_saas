package com.thinkerscave.staff.service;

import com.thinkerscave.staff.entity.Payroll;
import com.thinkerscave.staff.entity.StaffSalaryStructure;

/**
 * @deprecated Staff payslip PDF generation is retired. Use Finance Payroll {@code PayslipService}.
 * Not a Spring bean — retained only as a compile-time stub.
 */
@Deprecated
public class PayslipPdfService {

    public byte[] buildPayslip(Payroll payroll, StaffSalaryStructure structure) {
        throw new UnsupportedOperationException("Use Finance Payroll APIs");
    }
}
