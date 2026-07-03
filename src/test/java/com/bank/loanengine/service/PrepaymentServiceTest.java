package com.bank.loanengine.service;

import com.bank.loanengine.domain.BusinessOption;
import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import com.bank.loanengine.dto.PrepaymentRequest;
import com.bank.loanengine.dto.PrepaymentResponse;
import com.bank.loanengine.exception.InvalidPrepaymentException;
import com.bank.loanengine.exception.InstallmentNotFoundException;
import com.bank.loanengine.exception.LoanNotFoundException;
import com.bank.loanengine.messaging.event.PrepaymentAppliedEvent;
import com.bank.loanengine.messaging.producer.LoanEventProducer;
import com.bank.loanengine.repository.LoanRepository;
import com.bank.loanengine.service.strategy.PrepaymentContext;
import com.bank.loanengine.service.strategy.PrepaymentResult;
import com.bank.loanengine.service.strategy.PrepaymentStrategy;
import com.bank.loanengine.service.strategy.PrepaymentStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrepaymentServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private PrepaymentStrategyFactory strategyFactory;

    @Mock
    private LoanEventProducer eventProducer;

    @InjectMocks
    private PrepaymentService prepaymentService;

    @Captor
    private ArgumentCaptor<PrepaymentAppliedEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processPrepayment_appliesStrategyPersistsLoanAndPublishesEvent() {
        Loan loan = buildLoanWithSchedule(12, BigDecimal.valueOf(100000), BigDecimal.valueOf(12), LocalDate.of(2024, 1, 1));
        when(loanRepository.findWithScheduleById(1L)).thenReturn(Optional.of(loan));

        LoanScheduleInstallment trigger = loan.getSchedule().get(0);
        PrepaymentRequest request = new PrepaymentRequest(1, BigDecimal.valueOf(10000), BusinessOption.REDUCE_EMI_KEEP_TENOR);

        PrepaymentStrategy strategy = mock(PrepaymentStrategy.class);
        when(strategy.apply(any())).thenAnswer(invocation -> {
            PrepaymentContext context = invocation.getArgument(0);
            LoanScheduleInstallment triggerInstallment = context.triggerInstallment();
            triggerInstallment.setStatus(InstallmentStatus.PAID);
            triggerInstallment.setClosingBalance(context.outstandingPrincipalBeforePrepayment().subtract(context.prepaymentAmount()));
            return new PrepaymentResult(
                    context.outstandingPrincipalBeforePrepayment().subtract(context.prepaymentAmount()),
                    BigDecimal.valueOf(8200),
                    11,
                    "Test success"
            );
        });
        when(strategyFactory.resolve(BusinessOption.REDUCE_EMI_KEEP_TENOR)).thenReturn(strategy);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrepaymentResponse response = prepaymentService.processPrepayment(1L, request);

        assertThat(response.loanId()).isEqualTo(1L);
        assertThat(response.prepaymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(response.outstandingPrincipalAfter()).isEqualByComparingTo("90000.00");
        assertThat(response.message()).isEqualTo("Test success");
        assertThat(trigger.getStatus()).isEqualTo(InstallmentStatus.PAID);

        verify(loanRepository).save(any(Loan.class));
        verify(eventProducer).publishPrepaymentApplied(eventCaptor.capture());
        assertThat(eventCaptor.getValue().loanId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().prepaymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    void processPrepayment_loanNotFound_throwsLoanNotFoundException() {
        when(loanRepository.findWithScheduleById(1L)).thenReturn(Optional.empty());

        PrepaymentRequest request = new PrepaymentRequest(1, BigDecimal.valueOf(1000), BusinessOption.REDUCE_EMI_KEEP_TENOR);

        assertThatThrownBy(() -> prepaymentService.processPrepayment(1L, request))
                .isInstanceOf(LoanNotFoundException.class)
                .hasMessageContaining("Loan not found");
    }

    @Test
    void processPrepayment_invalidInstallmentNumber_throwsInvalidPrepaymentException() {
        Loan loan = buildLoanWithSchedule(12, BigDecimal.valueOf(100000), BigDecimal.valueOf(12), LocalDate.of(2024, 1, 1));
        when(loanRepository.findWithScheduleById(1L)).thenReturn(Optional.of(loan));

        int maxInstallment = loan.getSchedule().stream()
                .map(LoanScheduleInstallment::getInstallmentNumber)
                .max(Integer::compareTo)
                .orElse(loan.getTenorMonths());

        PrepaymentRequest request = new PrepaymentRequest(maxInstallment, BigDecimal.valueOf(1000), BusinessOption.REDUCE_EMI_KEEP_TENOR);

        assertThatThrownBy(() -> prepaymentService.processPrepayment(1L, request))
                .isInstanceOf(InvalidPrepaymentException.class)
                .hasMessageContaining("Cannot apply a prepayment at the final installment");
    }

    @Test
    void processPrepayment_amountExceedsOutstanding_throwsInvalidPrepaymentException() {
        Loan loan = buildLoanWithSchedule(12, BigDecimal.valueOf(100000), BigDecimal.valueOf(12), LocalDate.of(2024, 1, 1));
        when(loanRepository.findWithScheduleById(1L)).thenReturn(Optional.of(loan));

        PrepaymentRequest request = new PrepaymentRequest(1, BigDecimal.valueOf(200000), BusinessOption.REDUCE_EMI_KEEP_TENOR);

        assertThatThrownBy(() -> prepaymentService.processPrepayment(1L, request))
                .isInstanceOf(InvalidPrepaymentException.class)
                .hasMessageContaining("exceeds the outstanding principal");
    }

    private Loan buildLoanWithSchedule(int tenor, BigDecimal principal, BigDecimal annualRate, LocalDate startDate) {
        BigDecimal emi = LoanMath.calculateEmi(principal, annualRate, tenor);
        Loan loan = Loan.builder()
                .id(1L)
                .principalAmount(principal)
                .annualInterestRate(annualRate)
                .tenorMonths(tenor)
                .emiAmount(emi)
                .startDate(startDate)
                .build();

        List<LoanScheduleInstallment> schedule = new ScheduleGenerator().generateSchedule(
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
