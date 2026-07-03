package com.bank.loanengine.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Centralised financial-math helper. All monetary computations use {@link BigDecimal} with a
 * high-precision {@link MathContext} for intermediate steps, rounding only at the point a value
 * is persisted or returned to the client (HALF_UP to 2 decimal places), to avoid compounding
 * rounding error across a 60-month amortization schedule.
 */
public final class LoanMath {

    public static final int MONEY_SCALE = 2;
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    public static final MathContext CALC_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private LoanMath() {
    }

    /** Converts an annual percentage rate into a monthly decimal rate, e.g. 12% -> 0.01 */
    public static BigDecimal monthlyRate(BigDecimal annualRatePercent) {
        return annualRatePercent.divide(HUNDRED, CALC_CONTEXT).divide(TWELVE, CALC_CONTEXT);
    }

    /**
     * Standard reducing-balance EMI formula:
     * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     */
    public static BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRatePercent, int tenorMonths) {
        if (tenorMonths <= 0) {
            throw new IllegalArgumentException("tenorMonths must be greater than zero");
        }
        BigDecimal r = monthlyRate(annualRatePercent);

        if (r.compareTo(BigDecimal.ZERO) == 0) {
            return round(principal.divide(BigDecimal.valueOf(tenorMonths), CALC_CONTEXT));
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        BigDecimal onePlusRPowN = onePlusR.pow(tenorMonths, CALC_CONTEXT);

        BigDecimal numerator = principal.multiply(r, CALC_CONTEXT).multiply(onePlusRPowN, CALC_CONTEXT);
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE, CALC_CONTEXT);

        return round(numerator.divide(denominator, CALC_CONTEXT));
    }

    /** Interest due on an outstanding balance for a single month. */
    public static BigDecimal monthlyInterest(BigDecimal outstandingBalance, BigDecimal annualRatePercent) {
        BigDecimal r = monthlyRate(annualRatePercent);
        return round(outstandingBalance.multiply(r, CALC_CONTEXT));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }
}
