package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.enums.LopHandling;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import com.thinkerscave.finance.payroll.enums.SalaryRounding;
import com.thinkerscave.finance.payroll.enums.StatutoryCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayrollCalculationServiceTest {

    @Test
    @DisplayName("fixed + percent of basic + percent of gross")
    void calculatesFixedAndPercentages() {
        var lines = List.of(
                earning("BASIC", "Basic", CalculationMethod.FIXED_AMOUNT, "10000", 1),
                earning("HRA", "HRA", CalculationMethod.PERCENTAGE_OF_BASIC, "40", 2),
                earning("BONUS", "Bonus", CalculationMethod.PERCENTAGE_OF_GROSS, "10", 3),
                deduction("OTHER", "Other", CalculationMethod.FIXED_AMOUNT, "500", null, 4)
        );
        var ctx = new PayrollCalculationService.CalcContext(
                PaymentType.SALARY, true, true, true, true,
                30, 0, LopHandling.DEDUCT_FROM_BASIC, SalaryRounding.NEAREST_RUPEE);

        var result = PayrollCalculationService.calculate(lines, ctx);

        // basic 10000 + hra 4000 = 14000 provisional; +10% gross bonus = 1400 → gross 15400; deduct 500 → 14900
        assertEquals(0, new BigDecimal("15400.00").compareTo(result.grossAmount()));
        assertEquals(0, new BigDecimal("500.00").compareTo(result.totalDeductions()));
        assertEquals(0, new BigDecimal("14900.00").compareTo(result.netAmount()));
    }

    @Test
    @DisplayName("stipend skips disabled statutory even when line marked applicable")
    void stipendSkipsStatutoryWhenOrgDisabled() {
        var lines = List.of(
                earning("BASIC", "Basic", CalculationMethod.FIXED_AMOUNT, "8000", 1),
                deduction("PF", "PF", CalculationMethod.FIXED_AMOUNT, "960", StatutoryCode.PF, 2)
        );
        var ctx = new PayrollCalculationService.CalcContext(
                PaymentType.STIPEND, false, false, false, false,
                30, 0, LopHandling.DEDUCT_FROM_BASIC, SalaryRounding.NONE);

        var result = PayrollCalculationService.calculate(lines, ctx);

        assertEquals(0, new BigDecimal("8000.00").compareTo(result.grossAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalDeductions()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(result.netAmount()));
        assertTrue(result.lines().stream().noneMatch(l -> "PF".equals(l.code())));
    }

    @Test
    @DisplayName("employer contribution does not reduce net")
    void employerContributionInformationalOnly() {
        var lines = List.of(
                earning("BASIC", "Basic", CalculationMethod.FIXED_AMOUNT, "10000", 1),
                new PayrollCalculationService.InputLine(
                        3L, "EPF_ER", "Employer PF", ComponentType.EMPLOYER_CONTRIBUTION,
                        CalculationMethod.FIXED_AMOUNT, new BigDecimal("1200"), true,
                        StatutoryCode.PF, 2)
        );
        var ctx = new PayrollCalculationService.CalcContext(
                PaymentType.SALARY, true, false, false, false,
                30, 0, LopHandling.DEDUCT_FROM_BASIC, SalaryRounding.NONE);

        var result = PayrollCalculationService.calculate(lines, ctx);

        assertEquals(0, new BigDecimal("10000.00").compareTo(result.netAmount()));
        assertTrue(result.lines().stream().anyMatch(l -> "EPF_ER".equals(l.code())));
    }

    private static PayrollCalculationService.InputLine earning(
            String code, String name, CalculationMethod method, String value, int sort) {
        return new PayrollCalculationService.InputLine(
                (long) sort, code, name, ComponentType.EARNING, method,
                new BigDecimal(value), true, null, sort);
    }

    private static PayrollCalculationService.InputLine deduction(
            String code, String name, CalculationMethod method, String value,
            StatutoryCode statutory, int sort) {
        return new PayrollCalculationService.InputLine(
                (long) sort, code, name, ComponentType.DEDUCTION, method,
                new BigDecimal(value), true, statutory, sort);
    }
}
