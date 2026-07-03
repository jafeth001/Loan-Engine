package com.bank.loanengine.repository;

import com.bank.loanengine.domain.LoanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanTransactionRepository extends JpaRepository<LoanTransaction, Long> {

    List<LoanTransaction> findByLoanIdOrderByTransactionDateAsc(Long loanId);
}
