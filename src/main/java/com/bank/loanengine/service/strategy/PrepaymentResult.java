package com.bank.loanengine.service.strategy;

import java.math.BigDecimal;

public record PrepaymentResult(

        BigDecimal outstandingPrincipalAfter,

        BigDecimal newEmiAmount,

        int newRemainingTenorMonths,

        String message

) {
}