package com.bank.loanengine.repository;

import com.bank.loanengine.domain.LoanScheduleInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanScheduleInstallmentRepository extends JpaRepository<LoanScheduleInstallment, Long> {

    List<LoanScheduleInstallment> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);

    Optional<LoanScheduleInstallment> findByLoanIdAndInstallmentNumber(Long loanId, Integer installmentNumber);

    void deleteByLoanIdAndInstallmentNumberGreaterThan(Long loanId, Integer installmentNumber);
}
