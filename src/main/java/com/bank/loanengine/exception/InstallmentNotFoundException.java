package com.bank.loanengine.exception;

public class InstallmentNotFoundException extends RuntimeException {
    public InstallmentNotFoundException(Long loanId, Integer installmentNumber) {
        super("Installment " + installmentNumber + " not found for loan id: " + loanId);
    }
}
