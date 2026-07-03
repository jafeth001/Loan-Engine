package com.bank.loanengine.exception;

public class InvalidPrepaymentException extends RuntimeException {
    public InvalidPrepaymentException(String message) {
        super(message);
    }
}
