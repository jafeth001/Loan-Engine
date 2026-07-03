package com.bank.loanengine.service.strategy;

import com.bank.loanengine.domain.BusinessOption;
import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import com.bank.loanengine.exception.InvalidPrepaymentException;
import com.bank.loanengine.service.LoanMath;
import com.bank.loanengine.service.ScheduleGenerator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Option B: Reduce Tenor, Keep EMI.
 * The EMI amount stays fixed at the original value; the number of remaining installments is
 * shortened to reflect the lower outstanding principal. The remaining tenor is computed by
 * simulating the amortization at the existing EMI until the balance reaches (or would go below)
 * zero; the final simulated installment absorbs the residual balance exactly.
 */
@Component
public class ReduceTenorKeepEmiStrategy implements PrepaymentStrategy {

    private static final int SAFETY_MAX_MONTHS = 1200; // 100 years - guards against infinite loop

    private final ScheduleGenerator scheduleGenerator;

    public ReduceTenorKeepEmiStrategy(ScheduleGenerator scheduleGenerator) {
        this.scheduleGenerator = scheduleGenerator;
    }

    @Override
    public BusinessOption supportedOption() {
        return BusinessOption.REDUCE_TENOR_KEEP_EMI;
    }

    @Override
    public PrepaymentResult apply(PrepaymentContext context) {
        Loan loan = context.loan();
        LoanScheduleInstallment trigger = context.triggerInstallment();

        BigDecimal newPrincipal = context.outstandingPrincipalBeforePrepayment()
                .subtract(context.prepaymentAmount());

        BigDecimal fixedEmi = loan.getEmiAmount();
        BigDecimal monthlyInterestOnNewBalance = LoanMath.monthlyInterest(newPrincipal, loan.getAnnualInterestRate());

        if (fixedEmi.compareTo(monthlyInterestOnNewBalance) <= 0 && newPrincipal.compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidPrepaymentException(
                    "Existing EMI of " + fixedEmi + " is not sufficient to cover monthly interest of "
                            + monthlyInterestOnNewBalance + " on the reduced principal; the loan can never amortize at this EMI.");
        }

        int newRemainingTenor = simulateRemainingTenor(newPrincipal, loan.getAnnualInterestRate(), fixedEmi);

        int triggerNumber = trigger.getInstallmentNumber();

        // Remove old future schedule and regenerate at the fixed EMI over the shortened tenor.
        var removedInstallments = loan.getSchedule().stream()
                .filter(i -> i.getInstallmentNumber() > triggerNumber)
                .toList();
        loan.getSchedule().removeIf(i -> i.getInstallmentNumber() > triggerNumber);
        removedInstallments.forEach(i -> i.setLoan(null));

        List<LoanScheduleInstallment> newFutureSchedule = scheduleGenerator.generateSchedule(
                loan,
                newPrincipal,
                loan.getAnnualInterestRate(),
                fixedEmi,
                newRemainingTenor,
                triggerNumber + 1,
                trigger.getDueDate().plusMonths(1)
        );
        newFutureSchedule.forEach(i -> {
            i.setStatus(InstallmentStatus.ADJUSTED);
            loan.addInstallment(i);
        });

        trigger.setStatus(InstallmentStatus.PAID);
        trigger.setClosingBalance(newPrincipal);

        loan.setTenorMonths(triggerNumber + newRemainingTenor);
        // EMI amount on the loan stays the same (kept fixed), so no change to loan.emiAmount.

        return new PrepaymentResult(
                newPrincipal,
                fixedEmi,
                newRemainingTenor,
                String.format(
                        "Option B applied: principal reduced to %s, EMI kept at %s, remaining tenor shortened to %d months",
                        newPrincipal, fixedEmi, newRemainingTenor)
        );
    }

    /**
     * Simulates amortization at a fixed EMI to determine how many more months are needed to
     * bring the balance to zero (the last month may have a smaller true payment, which the
     * ScheduleGenerator accounts for when it builds the real schedule).
     */
    private int simulateRemainingTenor(BigDecimal openingBalance, BigDecimal annualRatePercent, BigDecimal emi) {
        BigDecimal balance = openingBalance;
        int months = 0;

        while (balance.compareTo(BigDecimal.ZERO) > 0 && months < SAFETY_MAX_MONTHS) {
            BigDecimal interest = LoanMath.monthlyInterest(balance, annualRatePercent);
            BigDecimal principalComponent = emi.subtract(interest);
            if (principalComponent.compareTo(BigDecimal.ZERO) <= 0) {
                // EMI does not even cover interest beyond this point; should already be guarded
                // against by the caller, but guard again defensively.
                throw new InvalidPrepaymentException("EMI is insufficient to amortize the remaining principal.");
            }
            balance = balance.subtract(principalComponent);
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balance = BigDecimal.ZERO;
            }
            months++;
        }

        if (months >= SAFETY_MAX_MONTHS) {
            throw new InvalidPrepaymentException("Unable to determine a reasonable remaining tenor for the given EMI.");
        }

        return months;
    }
}
