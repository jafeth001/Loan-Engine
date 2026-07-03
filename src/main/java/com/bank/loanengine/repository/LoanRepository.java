package com.bank.loanengine.repository;

import com.bank.loanengine.domain.Loan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @EntityGraph(attributePaths = {"schedule"})
    Optional<Loan> findWithScheduleById(Long id);
}
