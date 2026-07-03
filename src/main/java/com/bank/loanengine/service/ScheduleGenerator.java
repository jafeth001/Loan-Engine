package com.bank.loanengine.service;

import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a standard month-by-month reducing-balance amortization schedule.
 */
@Component
public class ScheduleGenerator {

    /**
     * Builds installments 1..tenorMonths starting from the given opening balance / date,
     * using a fixed EMI. The final installment absorbs any residual rounding difference so the
     * closing balance of the schedule is exactly zero.
     */
    public List<LoanScheduleInstallment> generateSchedule(
            Loan loan,
            BigDecimal openingPrincipal,
            BigDecimal annualRatePercent,
            BigDecimal emi,
            int tenorMonths,
            int startingInstallmentNumber,
            LocalDate firstDueDate
    ) {
        List<LoanScheduleInstallment> installments = new ArrayList<>();
        BigDecimal balance = openingPrincipal;
        LocalDate dueDate = firstDueDate;

        for (int i = 0; i < tenorMonths; i++) {
            int installmentNumber = startingInstallmentNumber + i;
            BigDecimal interest = LoanMath.monthlyInterest(balance, annualRatePercent);
            BigDecimal principalComponent;
            BigDecimal currentEmi;

            boolean isLastInstallment = (i == tenorMonths - 1);
            if (isLastInstallment) {
                // Final installment closes the loan exactly, absorbing rounding residue.
                principalComponent = balance;
                currentEmi = LoanMath.round(principalComponent.add(interest));
            } else {
                currentEmi = emi;
                principalComponent = LoanMath.round(currentEmi.subtract(interest));
            }

            BigDecimal closingBalance = LoanMath.round(balance.subtract(principalComponent));
            if (closingBalance.compareTo(BigDecimal.ZERO) < 0) {
                closingBalance = BigDecimal.ZERO.setScale(LoanMath.MONEY_SCALE);
            }

            LoanScheduleInstallment installment = LoanScheduleInstallment.builder()
                    .installmentNumber(installmentNumber)
                    .dueDate(dueDate)
                    .openingBalance(LoanMath.round(balance))
                    .emiAmount(currentEmi)
                    .principalComponent(principalComponent)
                    .interestComponent(interest)
                    .closingBalance(closingBalance)
                    .status(InstallmentStatus.PENDING)
                    .build();

            installments.add(installment);

            balance = closingBalance;
            dueDate = dueDate.plusMonths(1);
        }

        return installments;
    }
}
