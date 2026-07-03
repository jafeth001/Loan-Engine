package com.bank.loanengine.dto;

import com.bank.loanengine.domain.BusinessOption;

import java.math.BigDecimal;

public record PrepaymentResponse(
        Long loanId,
        BusinessOption optionApplied,
        Integer installmentNumber,
        BigDecimal prepaymentAmount,
        BigDecimal outstandingPrincipalBefore,
        BigDecimal outstandingPrincipalAfter,
        BigDecimal newEmiAmount,
        Integer newRemainingTenorMonths,
        String message,
        LoanResponse loan
) {
}
