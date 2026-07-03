package com.bank.loanengine.service.strategy;

import com.bank.loanengine.domain.BusinessOption;
import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import com.bank.loanengine.service.LoanMath;
import com.bank.loanengine.service.ScheduleGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Option A: Reduce EMI, Keep Tenor.
 * The prepayment reduces outstanding principal; remaining tenor is unchanged; EMI is
 * recalculated downward over the remaining months.
 */
@Component
@RequiredArgsConstructor
public class ReduceEmiKeepTenorStrategy implements PrepaymentStrategy {

    private final ScheduleGenerator scheduleGenerator;

    @Override
    public BusinessOption supportedOption() {
        return BusinessOption.REDUCE_EMI_KEEP_TENOR;
    }

    @Override
    public PrepaymentResult apply(PrepaymentContext context) {
        Loan loan = context.loan();
        LoanScheduleInstallment trigger = context.triggerInstallment();

        BigDecimal newPrincipal = context.outstandingPrincipalBeforePrepayment()
                .subtract(context.prepaymentAmount());

        int triggerNumber = trigger.getInstallmentNumber();
        int remainingTenor = loan.getTenorMonths() - triggerNumber;

        BigDecimal newEmi = LoanMath.calculateEmi(newPrincipal, loan.getAnnualInterestRate(), remainingTenor);

        // Remove the old future schedule (installments after the trigger) and clear the removed
        // installments' loan reference so JPA orphan-removal can delete them cleanly.
        var removedInstallments = loan.getSchedule().stream()
                .filter(i -> i.getInstallmentNumber() > triggerNumber)
                .toList();
        loan.getSchedule().removeIf(i -> i.getInstallmentNumber() > triggerNumber);
        removedInstallments.forEach(i -> i.setLoan(null));

        List<LoanScheduleInstallment> newFutureSchedule = scheduleGenerator.generateSchedule(
                loan,
                newPrincipal,
                loan.getAnnualInterestRate(),
                newEmi,
                remainingTenor,
                triggerNumber + 1,
                trigger.getDueDate().plusMonths(1)
        );
        newFutureSchedule.forEach(i -> {
            i.setStatus(InstallmentStatus.ADJUSTED);
            loan.addInstallment(i);
        });

        // Trigger installment itself is now fully paid (EMI + prepayment).
        trigger.setStatus(InstallmentStatus.PAID);
        trigger.setClosingBalance(newPrincipal);

        loan.setEmiAmount(newEmi);

        return new PrepaymentResult(
                newPrincipal,
                newEmi,
                remainingTenor,
                String.format(
                        "Option A applied: principal reduced to %s, tenor kept at %d remaining months, EMI recalculated to %s",
                        newPrincipal, remainingTenor, newEmi)
        );
    }
}
