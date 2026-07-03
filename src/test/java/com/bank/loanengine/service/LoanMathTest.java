package com.bank.loanengine.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanMathTest {

    @Test
    void monthlyRate_convertsAnnualRateToMonthlyDecimal() {
        BigDecimal monthly = LoanMath.monthlyRate(BigDecimal.valueOf(12));

        assertThat(monthly).isEqualByComparingTo("0.01");
    }

    @Test
    void calculateEmi_zeroInterest_returnsStraightLinePayment() {
        BigDecimal emi = LoanMath.calculateEmi(BigDecimal.valueOf(1200), BigDecimal.ZERO, 12);

        assertThat(emi).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateEmi_withInterest_returnsRoundedPayment() {
        BigDecimal emi = LoanMath.calculateEmi(BigDecimal.valueOf(100000), BigDecimal.valueOf(12), 12);

        assertThat(emi).isEqualByComparingTo("8884.88");
    }

    @Test
    void calculateEmi_invalidTenor_throwsException() {
        assertThatThrownBy(() -> LoanMath.calculateEmi(BigDecimal.valueOf(1000), BigDecimal.valueOf(12), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenorMonths must be greater than zero");
    }

    @Test
    void monthlyInterest_calculatesInterestOnBalance() {
        BigDecimal interest = LoanMath.monthlyInterest(BigDecimal.valueOf(100000), BigDecimal.valueOf(12));

        assertThat(interest).isEqualByComparingTo("1000.00");
    }

    @Test
    void round_roundsToTwoDecimalPlacesHalfUp() {
        assertThat(LoanMath.round(new BigDecimal("123.456"))).isEqualByComparingTo("123.46");
        assertThat(LoanMath.round(new BigDecimal("123.454"))).isEqualByComparingTo("123.45");
    }
}
