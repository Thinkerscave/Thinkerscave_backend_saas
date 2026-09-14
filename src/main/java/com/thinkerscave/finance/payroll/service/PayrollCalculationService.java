package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.enums.LopHandling;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import com.thinkerscave.finance.payroll.enums.SalaryRounding;
import com.thinkerscave.finance.payroll.enums.StatutoryCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Pure V1 payroll calculation engine (no Spring).
 * <p>
 * Order: BASIC fixed first → other FIXED / % of basic → provisional gross →
 * % of gross → LOP → employee deductions (employer contributions excluded from net) → round.
 * Employer contributions are informational and never reduce net pay.
 */
public final class PayrollCalculationService {

    private PayrollCalculationService() {
    }

    public record InputLine(
            Long salaryComponentId,
            String code,
            String name,
            ComponentType componentType,
            CalculationMethod calculationMethod,
            BigDecimal value,
            boolean applicable,
            StatutoryCode statutoryCode,
            int sortOrder
    ) {
    }

    public record OutputLine(
            Long salaryComponentId,
            String code,
            String name,
            ComponentType componentType,
            CalculationMethod calculationMethod,
            BigDecimal rateOrPercent,
            BigDecimal amount,
            int sortOrder
    ) {
    }

    public record Result(
            BigDecimal grossAmount,
            BigDecimal totalDeductions,
            BigDecimal netAmount,
            List<OutputLine> lines
    ) {
    }

    public record CalcContext(
            PaymentType paymentType,
            boolean pfEnabled,
            boolean esiEnabled,
            boolean professionalTaxEnabled,
            boolean tdsEnabled,
            Integer workingDays,
            Integer lopDays,
            LopHandling lopHandling,
            SalaryRounding salaryRounding
    ) {
    }

    public static Result calculate(List<InputLine> inputs, CalcContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        List<InputLine> applicable = inputs.stream()
                .filter(Objects::nonNull)
                .filter(InputLine::applicable)
                .filter(l -> isStatutoryAllowed(l, ctx))
                .sorted(Comparator
                        .comparingInt((InputLine l) -> calcPriority(l))
                        .thenComparingInt(InputLine::sortOrder)
                        .thenComparing(InputLine::code, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        BigDecimal basic = BigDecimal.ZERO;
        List<OutputLine> lines = new ArrayList<>();

        // Pass 1: BASIC fixed
        for (InputLine line : applicable) {
            if (line.componentType() != ComponentType.EARNING) {
                continue;
            }
            if (!"BASIC".equalsIgnoreCase(line.code()) || line.calculationMethod() != CalculationMethod.FIXED_AMOUNT
                    && line.calculationMethod() != CalculationMethod.MANUAL_ENTRY) {
                continue;
            }
            BigDecimal amount = nz(line.value());
            basic = amount;
            lines.add(out(line, amount, amount));
        }

        // Pass 2: other fixed / % basic earnings
        BigDecimal gross = basic;
        for (InputLine line : applicable) {
            if (line.componentType() != ComponentType.EARNING) {
                continue;
            }
            if ("BASIC".equalsIgnoreCase(line.code()) && (line.calculationMethod() == CalculationMethod.FIXED_AMOUNT
                    || line.calculationMethod() == CalculationMethod.MANUAL_ENTRY)) {
                continue;
            }
            if (line.calculationMethod() == CalculationMethod.PERCENTAGE_OF_GROSS) {
                continue;
            }
            BigDecimal amount = switch (line.calculationMethod()) {
                case FIXED_AMOUNT, MANUAL_ENTRY -> nz(line.value());
                case PERCENTAGE_OF_BASIC -> percentOf(basic, line.value());
                default -> BigDecimal.ZERO;
            };
            gross = gross.add(amount);
            lines.add(out(line, line.value(), amount));
        }

        // Pass 3: % of gross earnings
        BigDecimal grossBeforePctGross = gross;
        for (InputLine line : applicable) {
            if (line.componentType() != ComponentType.EARNING
                    || line.calculationMethod() != CalculationMethod.PERCENTAGE_OF_GROSS) {
                continue;
            }
            BigDecimal amount = percentOf(grossBeforePctGross, line.value());
            gross = gross.add(amount);
            lines.add(out(line, line.value(), amount));
        }

        // LOP
        int workingDays = ctx.workingDays() != null && ctx.workingDays() > 0 ? ctx.workingDays() : 30;
        int lopDays = ctx.lopDays() != null ? Math.max(0, ctx.lopDays()) : 0;
        if (lopDays > 0 && lopDays < workingDays) {
            if (ctx.lopHandling() == LopHandling.DEDUCT_FROM_BASIC) {
                BigDecimal daily = basic.divide(BigDecimal.valueOf(workingDays), 6, RoundingMode.HALF_UP);
                BigDecimal deduction = daily.multiply(BigDecimal.valueOf(lopDays));
                BigDecimal newBasic = basic.subtract(deduction).max(BigDecimal.ZERO);
                BigDecimal factor = basic.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : newBasic.divide(basic, 8, RoundingMode.HALF_UP);
                lines = scaleEarnings(lines, factor);
                gross = sumByType(lines, ComponentType.EARNING);
                basic = newBasic;
            } else {
                // PRORATE_GROSS (default)
                BigDecimal factor = BigDecimal.valueOf(workingDays - lopDays)
                        .divide(BigDecimal.valueOf(workingDays), 8, RoundingMode.HALF_UP);
                lines = scaleEarnings(lines, factor);
                gross = sumByType(lines, ComponentType.EARNING);
            }
        } else if (lopDays >= workingDays) {
            lines = scaleEarnings(lines, BigDecimal.ZERO);
            gross = BigDecimal.ZERO;
        }

        // Deductions (employee only)
        BigDecimal deductions = BigDecimal.ZERO;
        for (InputLine line : applicable) {
            if (line.componentType() != ComponentType.DEDUCTION) {
                continue;
            }
            BigDecimal amount = switch (line.calculationMethod()) {
                case FIXED_AMOUNT, MANUAL_ENTRY -> nz(line.value());
                case PERCENTAGE_OF_BASIC -> percentOf(basic, line.value());
                case PERCENTAGE_OF_GROSS -> percentOf(gross, line.value());
            };
            deductions = deductions.add(amount);
            lines.add(out(line, line.value(), amount));
        }

        // Employer contributions — informational only; do not affect net
        for (InputLine line : applicable) {
            if (line.componentType() != ComponentType.EMPLOYER_CONTRIBUTION) {
                continue;
            }
            BigDecimal amount = switch (line.calculationMethod()) {
                case FIXED_AMOUNT, MANUAL_ENTRY -> nz(line.value());
                case PERCENTAGE_OF_BASIC -> percentOf(basic, line.value());
                case PERCENTAGE_OF_GROSS -> percentOf(gross, line.value());
            };
            lines.add(out(line, line.value(), amount));
        }

        BigDecimal net = gross.subtract(deductions).max(BigDecimal.ZERO);
        if (ctx.salaryRounding() == SalaryRounding.NEAREST_RUPEE) {
            gross = roundRupee(gross);
            deductions = roundRupee(deductions);
            net = roundRupee(net);
            lines = lines.stream()
                    .map(l -> new OutputLine(l.salaryComponentId(), l.code(), l.name(), l.componentType(),
                            l.calculationMethod(), l.rateOrPercent(), roundRupee(l.amount()), l.sortOrder()))
                    .toList();
        } else {
            gross = scale2(gross);
            deductions = scale2(deductions);
            net = scale2(net);
        }

        return new Result(gross, deductions, net, List.copyOf(lines));
    }

    private static boolean isStatutoryAllowed(InputLine line, CalcContext ctx) {
        if (line.statutoryCode() == null) {
            return true;
        }
        // Stipend: skip statutory unless explicitly applicable (already filtered by applicable flag)
        // Org toggles further gate statutory components
        return switch (line.statutoryCode()) {
            case PF -> Boolean.TRUE.equals(ctx.pfEnabled());
            case ESI -> Boolean.TRUE.equals(ctx.esiEnabled());
            case PT -> Boolean.TRUE.equals(ctx.professionalTaxEnabled());
            case TDS -> Boolean.TRUE.equals(ctx.tdsEnabled());
        };
    }

    private static int calcPriority(InputLine line) {
        if (line.componentType() == ComponentType.EARNING && "BASIC".equalsIgnoreCase(line.code())) {
            return 0;
        }
        if (line.componentType() == ComponentType.EARNING
                && line.calculationMethod() != CalculationMethod.PERCENTAGE_OF_GROSS) {
            return 1;
        }
        if (line.componentType() == ComponentType.EARNING) {
            return 2;
        }
        if (line.componentType() == ComponentType.DEDUCTION) {
            return 3;
        }
        return 4;
    }

    private static List<OutputLine> scaleEarnings(List<OutputLine> lines, BigDecimal factor) {
        List<OutputLine> out = new ArrayList<>();
        for (OutputLine l : lines) {
            if (l.componentType() == ComponentType.EARNING) {
                out.add(new OutputLine(l.salaryComponentId(), l.code(), l.name(), l.componentType(),
                        l.calculationMethod(), l.rateOrPercent(),
                        scale2(l.amount().multiply(factor)), l.sortOrder()));
            } else {
                out.add(l);
            }
        }
        return out;
    }

    private static BigDecimal sumByType(List<OutputLine> lines, ComponentType type) {
        return lines.stream()
                .filter(l -> l.componentType() == type)
                .map(OutputLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static OutputLine out(InputLine line, BigDecimal rate, BigDecimal amount) {
        return new OutputLine(
                line.salaryComponentId(),
                line.code(),
                line.name(),
                line.componentType(),
                line.calculationMethod(),
                rate,
                scale2(amount),
                line.sortOrder());
    }

    private static BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        return scale2(nz(base).multiply(nz(percent)).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal scale2(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal roundRupee(BigDecimal v) {
        return nz(v).setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }
}
