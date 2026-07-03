package com.bank.loanengine.dto;

import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.domain.LoanScheduleInstallment;
import com.bank.loanengine.domain.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public record LoanResponse(
        Long id,
        BigDecimal principalAmount,
        BigDecimal annualInterestRate,
        Integer tenorMonths,
        BigDecimal emiAmount,
        LoanStatus status,
        LocalDate startDate,
        List<InstallmentResponse> schedule
) {
    public static LoanResponse from(Loan loan) {
        List<InstallmentResponse> installments = loan.getSchedule() == null ? List.of() :
                loan.getSchedule().stream()
                        .sorted((a, b) -> a.getInstallmentNumber().compareTo(b.getInstallmentNumber()))
                        .map(InstallmentResponse::from)
                        .collect(Collectors.toList());

        return new LoanResponse(
                loan.getId(),
                loan.getPrincipalAmount(),
                loan.getAnnualInterestRate(),
                loan.getTenorMonths(),
                loan.getEmiAmount(),
                loan.getStatus(),
                loan.getStartDate(),
                installments
        );
    }

    public record InstallmentResponse(
            Integer installmentNumber,
            LocalDate dueDate,
            BigDecimal openingBalance,
            BigDecimal emiAmount,
            BigDecimal principalComponent,
            BigDecimal interestComponent,
            BigDecimal closingBalance,
            String status
    ) {
        public static InstallmentResponse from(LoanScheduleInstallment i) {
            return new InstallmentResponse(
                    i.getInstallmentNumber(),
                    i.getDueDate(),
                    i.getOpeningBalance(),
                    i.getEmiAmount(),
                    i.getPrincipalComponent(),
                    i.getInterestComponent(),
                    i.getClosingBalance(),
                    i.getStatus().name()
            );
        }
    }
}
