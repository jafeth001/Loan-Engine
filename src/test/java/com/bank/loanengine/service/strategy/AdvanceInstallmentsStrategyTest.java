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

class AdvanceInstallmentsStrategyTest {

    private final ScheduleGenerator scheduleGenerator = new ScheduleGenerator();
    private final AdvanceInstallmentsStrategy strategy = new AdvanceInstallmentsStrategy();

    @Test
    void apply_marksFutureInstallmentsAsAdvancedAndKeepsOutstandingPrincipal() {
        Loan loan = buildLoanWithSchedule(12, BigDecimal.valueOf(100000), BigDecimal.valueOf(12), LocalDate.of(2024, 1, 1));
        LoanScheduleInstallment trigger = loan.getSchedule().get(0);

        BigDecimal prepaymentAmount = loan.getEmiAmount().multiply(BigDecimal.valueOf(2));
        PrepaymentContext context = new PrepaymentContext(
                loan,
                trigger,
                prepaymentAmount,
                trigger.getOpeningBalance()
        );

        PrepaymentResult result = strategy.apply(context);

        assertThat(result.outstandingPrincipalAfter()).isEqualByComparingTo(trigger.getOpeningBalance());
        assertThat(result.newEmiAmount()).isEqualByComparingTo(loan.getEmiAmount());
        assertThat(result.newRemainingTenorMonths()).isEqualTo(loan.getTenorMonths() - trigger.getInstallmentNumber());
        assertThat(trigger.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(loan.getSchedule()).hasSize(12);
        assertThat(loan.getSchedule().subList(1, loan.getSchedule().size()))
                .filteredOn(i -> i.getStatus() == InstallmentStatus.ADVANCED)
                .hasSize(2);
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
