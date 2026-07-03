package com.bank.loanengine.service;

import com.bank.loanengine.domain.*;
import com.bank.loanengine.dto.LoanResponse;
import com.bank.loanengine.dto.PrepaymentRequest;
import com.bank.loanengine.dto.PrepaymentResponse;
import com.bank.loanengine.exception.InstallmentNotFoundException;
import com.bank.loanengine.exception.InvalidPrepaymentException;
import com.bank.loanengine.exception.LoanNotFoundException;
import com.bank.loanengine.messaging.event.PrepaymentAppliedEvent;
import com.bank.loanengine.messaging.producer.LoanEventProducer;
import com.bank.loanengine.repository.LoanRepository;
import com.bank.loanengine.service.strategy.PrepaymentContext;
import com.bank.loanengine.service.strategy.PrepaymentResult;
import com.bank.loanengine.service.strategy.PrepaymentStrategy;
import com.bank.loanengine.service.strategy.PrepaymentStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrepaymentService {

    private static final Logger log = LoggerFactory.getLogger(PrepaymentService.class);

    private final LoanRepository           loanRepository;
    private final PrepaymentStrategyFactory strategyFactory;
    private final LoanEventProducer         eventProducer;

    /**
     * schedule, logs an immutable transaction record, and publishes a Kafka event. The entire
     * DB operation is wrapped in a single transaction so partial failures roll back cleanly.
     */
    @Transactional
    public PrepaymentResponse processPrepayment(Long loanId, PrepaymentRequest request) {
        log.info("Processing prepayment for loan {}: installment={}, amount={}, option={}",
                loanId, request.installmentNumber(), request.amount(), request.option());
        Loan loan = loanRepository.findWithScheduleById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
        log.debug("Fetched loan {} with schedule size={}", loanId, loan.getSchedule() == null ? 0 : loan.getSchedule().size());

        LoanScheduleInstallment trigger = loan.getSchedule().stream()
                .filter(i -> i.getInstallmentNumber().equals(request.installmentNumber()))
                .findFirst()
                .orElseThrow(() -> new InstallmentNotFoundException(loanId, request.installmentNumber()));

        validate(loan, trigger, request);

        BigDecimal outstandingBefore = trigger.getOpeningBalance();

        PrepaymentContext context = new PrepaymentContext(
                loan, trigger, request.amount(), outstandingBefore);

        PrepaymentStrategy strategy = strategyFactory.resolve(request.option());
        PrepaymentResult   result   = strategy.apply(context);

        // Immutable transaction log entry
        LoanTransaction tx = LoanTransaction.builder()
                .transactionType(TransactionType.PREPAYMENT)
                .businessOption(request.option())
                .amount(request.amount())
                .installmentNumber(request.installmentNumber())
                .balanceBefore(outstandingBefore)
                .balanceAfter(result.outstandingPrincipalAfter())
                .notes(result.message())
                .build();
        loan.addTransaction(tx);

        Loan saved = loanRepository.save(loan);

        // Kafka event (published after successful DB commit via @Transactional boundary)
        eventProducer.publishPrepaymentApplied(new PrepaymentAppliedEvent(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                saved.getId(),
                request.option(),
                request.installmentNumber(),
                request.amount(),
                outstandingBefore,
                result.outstandingPrincipalAfter(),
                result.newEmiAmount(),
                result.newRemainingTenorMonths(),
                result.message()
        ));

        return new PrepaymentResponse(
                saved.getId(),
                request.option(),
                request.installmentNumber(),
                request.amount(),
                outstandingBefore,
                result.outstandingPrincipalAfter(),
                result.newEmiAmount(),
                result.newRemainingTenorMonths(),
                result.message(),
                LoanResponse.from(saved)
        );
    }

    // ── Validation ───────────────────────────────────────────────────────────────────────────

    private void validate(Loan loan, LoanScheduleInstallment trigger, PrepaymentRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPrepaymentException("Prepayment amount must be positive.");
        }

        int maxInstallment = loan.getSchedule().stream()
                .map(LoanScheduleInstallment::getInstallmentNumber)
                .max(Comparator.naturalOrder())
                .orElse(loan.getTenorMonths());

        if (request.installmentNumber() < 1 || request.installmentNumber() > maxInstallment) {
            throw new InvalidPrepaymentException(
                    "installmentNumber must be between 1 and " + maxInstallment + " for this loan.");
        }

        if (request.installmentNumber().equals(maxInstallment)) {
            throw new InvalidPrepaymentException(
                    "Cannot apply a prepayment at the final installment.");
        }

        BigDecimal outstanding = trigger.getOpeningBalance();
        if (request.amount().compareTo(outstanding) > 0) {
            throw new InvalidPrepaymentException(
                    "Prepayment amount " + request.amount()
                            + " exceeds the outstanding principal of " + outstanding
                            + " at installment " + request.installmentNumber() + ".");
        }
    }
}
