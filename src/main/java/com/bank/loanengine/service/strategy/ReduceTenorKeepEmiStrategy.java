package com.bank.loanengine.service.strategy;

import com.bank.loanengine.domain.BusinessOption;
import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import com.bank.loanengine.exception.InvalidPrepaymentException;
import com.bank.loanengine.service.LoanMath;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Option B: Reduce Tenor, Keep EMI.
 *
 * Strategy performs calculations ONLY.
 * No EntityManager.
 * No repository.
 * No schedule generation.
 * No persistence.
 */
@Component
public class ReduceTenorKeepEmiStrategy implements PrepaymentStrategy {

    private static final int SAFETY_MAX_MONTHS = 1200;

    @Override
    public BusinessOption supportedOption() {
        return BusinessOption.REDUCE_TENOR_KEEP_EMI;
    }

    @Override
    public PrepaymentResult apply(PrepaymentContext context) {

        Loan loan = context.loan();
        LoanScheduleInstallment trigger = context.triggerInstallment();

        BigDecimal newPrincipal =
                context.outstandingPrincipalBeforePrepayment()
                        .subtract(context.prepaymentAmount());

        BigDecimal fixedEmi = loan.getEmiAmount();

        BigDecimal monthlyInterest =
                LoanMath.monthlyInterest(
                        newPrincipal,
                        loan.getAnnualInterestRate());

        if (fixedEmi.compareTo(monthlyInterest) <= 0 &&
                newPrincipal.compareTo(BigDecimal.ZERO) > 0) {

            throw new InvalidPrepaymentException(
                    "Existing EMI is too small to amortize the remaining balance."
            );
        }

        int newRemainingTenor =
                simulateRemainingTenor(
                        newPrincipal,
                        loan.getAnnualInterestRate(),
                        fixedEmi);

        // Update the trigger installment status to PAID
        trigger.setStatus(InstallmentStatus.PAID);
        
        return new PrepaymentResult(
                newPrincipal,
                fixedEmi,
                newRemainingTenor,
                String.format(
                        "Option B applied: principal reduced to %s, EMI unchanged, remaining tenor reduced to %d months.",
                        newPrincipal,
                        newRemainingTenor
                )
        );
    }

    private int simulateRemainingTenor(
            BigDecimal balance,
            BigDecimal annualRate,
            BigDecimal emi) {

        int months = 0;

        while (balance.compareTo(BigDecimal.ZERO) > 0 &&
                months < SAFETY_MAX_MONTHS) {

            BigDecimal interest =
                    LoanMath.monthlyInterest(balance, annualRate);

            BigDecimal principal =
                    emi.subtract(interest);

            if (principal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidPrepaymentException(
                        "Loan cannot amortize using the current EMI.");
            }

            balance = balance.subtract(principal);

            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balance = BigDecimal.ZERO;
            }

            months++;
        }

        if (months >= SAFETY_MAX_MONTHS) {
            throw new InvalidPrepaymentException(
                    "Unable to calculate remaining tenor.");
        }

        return months;
    }
}