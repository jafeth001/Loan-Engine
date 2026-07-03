package com.bank.loanengine.service.strategy;

import com.bank.loanengine.domain.BusinessOption;
import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import com.bank.loanengine.service.LoanMath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Option A: Reduce EMI, Keep Tenor.
 *
 * This strategy ONLY performs business calculations.
 */
@Component
@RequiredArgsConstructor
public class ReduceEmiKeepTenorStrategy implements PrepaymentStrategy {

    @Override
    public BusinessOption supportedOption() {
        return BusinessOption.REDUCE_EMI_KEEP_TENOR;
    }

    @Override
    public PrepaymentResult apply(PrepaymentContext context) {

        Loan loan = context.loan();
        LoanScheduleInstallment trigger = context.triggerInstallment();

        BigDecimal newPrincipal =
                context.outstandingPrincipalBeforePrepayment()
                        .subtract(context.prepaymentAmount());

        int remainingTenor =
                loan.getTenorMonths() - trigger.getInstallmentNumber();

        BigDecimal newEmi =
                LoanMath.calculateEmi(
                        newPrincipal,
                        loan.getAnnualInterestRate(),
                        remainingTenor
                );

        // Update the trigger installment status to PAID
        trigger.setStatus(InstallmentStatus.PAID);
        return new PrepaymentResult(
                newPrincipal,
                newEmi,
                remainingTenor,
                String.format(
                        "Option A applied: principal reduced to %s, tenor unchanged, EMI recalculated to %s",
                        newPrincipal,
                        newEmi
                )
        );
    }
}