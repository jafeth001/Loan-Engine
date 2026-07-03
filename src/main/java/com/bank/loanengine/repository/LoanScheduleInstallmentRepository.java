package com.bank.loanengine.repository;

import com.bank.loanengine.domain.LoanScheduleInstallment;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LoanScheduleInstallmentRepository extends JpaRepository<LoanScheduleInstallment, Long> {

    @Modifying
    @Transactional
    @Query("""
            delete from LoanScheduleInstallment i
            where i.loan.id = :loanId
            and i.installmentNumber > :trigger
            """)
    void deleteFutureInstallments(Long loanId, Integer trigger);
}