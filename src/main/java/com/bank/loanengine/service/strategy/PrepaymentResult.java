package com.bank.loanengine.service.strategy;

import java.math.BigDecimal;

public record PrepaymentResult(
        BigDecimal outstandingPrincipalAfter,
        BigDecimal newEmiAmount,
        Integer newRemainingTenorMonths,
        String message
) {
}
