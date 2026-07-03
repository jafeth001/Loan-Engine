package com.bank.loanengine.service;

import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleGeneratorTest {

    private final ScheduleGenerator generator = new ScheduleGenerator();

    @Test
    void generateSchedule_buildsFullAmortizationScheduleWithZeroFinalBalance() {
        Loan loan = Loan.builder()
                .principalAmount(BigDecimal.valueOf(1000))
                .annualInterestRate(BigDecimal.valueOf(12))
                .tenorMonths(2)
                .build();

        BigDecimal emi = LoanMath.calculateEmi(BigDecimal.valueOf(1000), BigDecimal.valueOf(12), 2);
        LocalDate firstDueDate = LocalDate.of(2024, 1, 1);

        List<LoanScheduleInstallment> schedule = generator.generateSchedule(
                loan,
                loan.getPrincipalAmount(),
                loan.getAnnualInterestRate(),
                emi,
                loan.getTenorMonths(),
                1,
                firstDueDate
        );

        assertThat(schedule).hasSize(2);
        assertThat(schedule.get(0).getOpeningBalance()).isEqualByComparingTo("1000.00");
        assertThat(schedule.get(0).getStatus()).isEqualTo(InstallmentStatus.PENDING);
        assertThat(schedule.get(1).getClosingBalance()).isEqualByComparingTo("0.00");
        assertThat(schedule.get(1).getEmiAmount()).isEqualByComparingTo(LoanMath.round(
                schedule.get(1).getPrincipalComponent().add(schedule.get(1).getInterestComponent())));
    }
}
