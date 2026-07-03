package com.bank.loanengine.service.strategy;

import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import com.bank.loanengine.service.LoanMath;
import com.bank.loanengine.service.ScheduleGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReduceTenorKeepEmiStrategyTest {

    private final ScheduleGenerator scheduleGenerator = new ScheduleGenerator();
    private final ReduceTenorKeepEmiStrategy strategy = new ReduceTenorKeepEmiStrategy(scheduleGenerator);

    @Test
    void apply_shortensRemainingTenorWhileKeepingEmiFixed() {
        Loan loan = buildLoanWithSchedule(12, BigDecimal.valueOf(100000), BigDecimal.valueOf(12), LocalDate.of(2024, 1, 1));
        LoanScheduleInstallment trigger = loan.getSchedule().get(0);

        BigDecimal prepaymentAmount = BigDecimal.valueOf(10000);
        PrepaymentContext context = new PrepaymentContext(
                loan,
                trigger,
                prepaymentAmount,
                trigger.getOpeningBalance()
        );

        PrepaymentResult result = strategy.apply(context);

        assertThat(result.newRemainingTenorMonths()).isLessThanOrEqualTo(11);
        assertThat(result.newEmiAmount()).isEqualByComparingTo(loan.getEmiAmount());
        assertThat(result.outstandingPrincipalAfter()).isEqualByComparingTo("90000.00");
        assertThat(trigger.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(loan.getSchedule()).hasSize(1 + result.newRemainingTenorMonths());
        assertThat(loan.getSchedule().subList(1, loan.getSchedule().size()))
                .allMatch(i -> i.getStatus() == InstallmentStatus.ADJUSTED);
        assertThat(loan.getTenorMonths()).isEqualTo(1 + result.newRemainingTenorMonths());
    }

    private Loan buildLoanWithSchedule(int tenor, BigDecimal principal, BigDecimal annualRate, LocalDate startDate) {
        BigDecimal emi = LoanMath.calculateEmi(principal, annualRate, tenor);
        Loan loan = Loan.builder()
                .principalAmount(principal)
                .annualInterestRate(annualRate)
                .tenorMonths(tenor)
                .emiAmount(emi)
                .startDate(startDate)
                .build();

        List<LoanScheduleInstallment> schedule = scheduleGenerator.generateSchedule(
                loan,
                principal,
                annualRate,
                emi,
                tenor,
                1,
                startDate.plusMonths(1)
        );
        schedule.forEach(loan::addInstallment);
        return loan;
    }
}
