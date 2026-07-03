package com.bank.loanengine.service;

import com.bank.loanengine.config.RedisConfig;
import com.bank.loanengine.domain.InstallmentStatus;
import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import com.bank.loanengine.domain.LoanStatus;
import com.bank.loanengine.dto.CreateLoanRequest;
import com.bank.loanengine.exception.LoanNotFoundException;
import com.bank.loanengine.messaging.event.LoanCreatedEvent;
import com.bank.loanengine.messaging.producer.LoanEventProducer;
import com.bank.loanengine.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository     loanRepository;
    private final ScheduleGenerator  scheduleGenerator;
    private final LoanEventProducer  eventProducer;

    // ── Create ───────────────────────────────────────────────────────────────────────────────

    @Transactional
    public Loan createLoan(CreateLoanRequest request) {
        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();
        var emi = LoanMath.calculateEmi(
                request.principalAmount(), request.annualInterestRate(), request.tenorMonths());

        Loan loan = Loan.builder()
                .principalAmount(request.principalAmount())
                .annualInterestRate(request.annualInterestRate())
                .tenorMonths(request.tenorMonths())
                .emiAmount(emi)
                .status(LoanStatus.ACTIVE)
                .startDate(startDate)
                .build();

        List<LoanScheduleInstallment> schedule = scheduleGenerator.generateSchedule(
                loan, request.principalAmount(), request.annualInterestRate(),
                emi, request.tenorMonths(), 1, startDate.plusMonths(1));
        schedule.forEach(loan::addInstallment);

        Loan saved = loanRepository.save(loan);

        // Publish Kafka event after successful commit (fire-and-forget).
        eventProducer.publishLoanCreated(new LoanCreatedEvent(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                saved.getId(),
                saved.getPrincipalAmount(),
                saved.getAnnualInterestRate(),
                saved.getTenorMonths(),
                saved.getEmiAmount(),
                saved.getStartDate()
        ));

        return saved;
    }

    // ── Read (cached) ─────────────────────────────────────────────────────────────────────────

        @Cacheable(value = RedisConfig.CACHE_LOANS, key = "#loanId")
        @Transactional(readOnly = true)
        public com.bank.loanengine.dto.LoanResponse getLoan(Long loanId) {
                Loan loan = loanRepository.findWithScheduleById(loanId)
                                .orElseThrow(() -> new LoanNotFoundException(loanId));
                return com.bank.loanengine.dto.LoanResponse.from(loan);
        }

    // ── Mark paid-up-to (evicts cache) ───────────────────────────────────────────────────────

    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_LOANS,          key = "#loanId"),
            @CacheEvict(value = RedisConfig.CACHE_LOAN_SCHEDULES, key = "#loanId")
    })
    @Transactional
    public void markPaidUpTo(Long loanId, int installmentNumber) {
        Loan loan = loanRepository.findWithScheduleById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
        loan.getSchedule().stream()
                .filter(i -> i.getInstallmentNumber() <= installmentNumber)
                .forEach(i -> i.setStatus(InstallmentStatus.PAID));
        loanRepository.save(loan);
    }
}
