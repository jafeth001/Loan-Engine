package com.bank.loanengine.service.strategy;

import com.bank.loanengine.domain.BusinessOption;
import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import com.bank.loanengine.repository.LoanRepository;
import com.bank.loanengine.repository.LoanScheduleInstallmentRepository;
import com.bank.loanengine.service.LoanMath;
import com.bank.loanengine.service.ScheduleGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReduceEmiKeepTenorStrategyTest {

    private final ScheduleGenerator scheduleGenerator = new ScheduleGenerator();
    private final LoanScheduleInstallmentRepository installmentRepository = Mockito.mock(LoanScheduleInstallmentRepository.class);
    private final LoanRepository loanRepository = Mockito.mock(LoanRepository.class);
    private final ReduceEmiKeepTenorStrategy strategy = new ReduceEmiKeepTenorStrategy();

    public ReduceEmiKeepTenorStrategyTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void apply_recalculatesEmiAndKeepsRemainingTenor() {
        Loan loan = buildLoanWithSchedule(12, BigDecimal.valueOf(100000), BigDecimal.valueOf(12), LocalDate.of(2024, 1, 1));
        LoanScheduleInstallment trigger = loan.getSchedule().get(0);
        BigDecimal originalEmi = loan.getEmiAmount();

        BigDecimal prepaymentAmount = BigDecimal.valueOf(10000);
        PrepaymentContext context = new PrepaymentContext(
                loan,
                trigger,
                prepaymentAmount,
                trigger.getOpeningBalance()
        );

        PrepaymentResult result = strategy.apply(context);

        assertThat(result.newEmiAmount()).isLessThan(originalEmi);

        assertThat(result.outstandingPrincipalAfter()).isEqualByComparingTo("90000.00");

        assertThat(result.newRemainingTenorMonths()).isEqualTo(11);
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
