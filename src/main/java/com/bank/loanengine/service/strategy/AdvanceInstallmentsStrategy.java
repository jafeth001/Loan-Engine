package com.bank.loanengine.service.strategy;

import com.bank.loanengine.domain.BusinessOption;
import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Option C: Advance Installments (No Recalculation).
 * The prepayment does NOT touch the principal or the schedule's interest/principal split.
 * Instead, it is treated as a pool of cash that is consumed, future-EMI by future-EMI, marking
 * each covered installment as ADVANCED (pre-paid). The loan continues to run for its full
 * original tenor and interest accrues exactly as originally scheduled.
 */
@Component
public class AdvanceInstallmentsStrategy implements PrepaymentStrategy {

    @Override
    public BusinessOption supportedOption() {
        return BusinessOption.ADVANCE_INSTALLMENTS;
    }

    @Override
    public PrepaymentResult apply(PrepaymentContext context) {
        Loan loan = context.loan();
        LoanScheduleInstallment trigger = context.triggerInstallment();
        BigDecimal pool = context.prepaymentAmount();

        // The trigger installment's own EMI is treated as already settled (per the assessment's
        // assumption that all installments up to and including the trigger have been paid).
        trigger.setStatus(InstallmentStatus.PAID);

        List<LoanScheduleInstallment> futureInstallments = loan.getSchedule().stream()
                .filter(i -> i.getInstallmentNumber() > trigger.getInstallmentNumber())
                .sorted((a, b) -> a.getInstallmentNumber().compareTo(b.getInstallmentNumber()))
                .toList();

        int advancedCount = 0;
        BigDecimal remainingPool = pool;

        for (LoanScheduleInstallment installment : futureInstallments) {
            BigDecimal emi = installment.getEmiAmount();
            if (remainingPool.compareTo(emi) >= 0) {
                installment.setStatus(InstallmentStatus.ADVANCED);
                remainingPool = remainingPool.subtract(emi);
                advancedCount++;
            } else {
                break; // not enough left in the pool to cover the next full EMI
            }
        }

        // Principal, EMI and tenor are unchanged under this option.
        BigDecimal unchangedOutstanding = context.outstandingPrincipalBeforePrepayment();

        String message = String.format(
                "Option C applied: %s held as a pool of future installments, %d future EMI(s) marked as advanced, "
                        + "%s remains as unallocated credit. Principal (%s), EMI (%s) and tenor (%d months) are unchanged; "
                        + "interest continues to accrue per the original schedule.",
                pool, advancedCount, remainingPool, unchangedOutstanding, loan.getEmiAmount(), loan.getTenorMonths());

        return new PrepaymentResult(
                unchangedOutstanding,
                loan.getEmiAmount(),
                loan.getTenorMonths() - trigger.getInstallmentNumber(),
                message
        );
    }
}
