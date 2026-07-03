package com.bank.loanengine.domain;

public enum InstallmentStatus {
    PENDING,
    PAID,
    ADJUSTED,   // schedule row was recalculated due to a prepayment (Options A/B)
    ADVANCED,   // installment pre-paid in advance via the prepayment pool (Option C)
}
