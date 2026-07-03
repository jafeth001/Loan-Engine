package com.bank.loanengine.service.strategy;

import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;

import java.math.BigDecimal;

/**
 * Immutable bundle of everything a {@link PrepaymentStrategy} needs to act on a prepayment
 * event. Keeping this as a single context object means new strategies can be added without
 * changing every existing strategy's method signature.
 */
public record PrepaymentContext(
        Loan loan,
        LoanScheduleInstallment triggerInstallment,  // the installment at which the prepayment occurs
        BigDecimal prepaymentAmount,
        BigDecimal outstandingPrincipalBeforePrepayment
) {
}
