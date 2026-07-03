package com.bank.loanengine.controller;

import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.dto.CreateLoanRequest;
import com.bank.loanengine.dto.LoanResponse;
import com.bank.loanengine.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Loans",
        description = "Create loans, retrieve loan details and amortisation schedules, "
                + "and simulate prior payments before applying a prepayment event."
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;


    // ── Create ────────────────────────────────────────────────────────────────

    @Operation(summary = "Create a loan (ADMIN only)")
    @PostMapping
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        Loan loan = loanService.createLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.from(loan));
    }

    // ── Get loan ──────────────────────────────────────────────────────────────

    @Operation(summary = "Get a loan with its schedule")
        @GetMapping("/{loanId}")
        public ResponseEntity<LoanResponse> getLoan(
            @Parameter(description = "Loan ID", example = "1", required = true)
            @PathVariable Long loanId
    ) {
                return ResponseEntity.ok(loanService.getLoan(loanId));
    }

    // ── Get schedule ──────────────────────────────────────────────────────────

    @Operation(summary = "Get a loan schedule")
    @GetMapping("/{loanId}/schedule")
    public ResponseEntity<LoanResponse> getSchedule(
            @Parameter(description = "Loan ID", example = "1", required = true)
            @PathVariable Long loanId
    ) {
                return ResponseEntity.ok(loanService.getLoan(loanId));
    }

    // ── Mark paid up to ───────────────────────────────────────────────────────

    @Operation(summary = "Mark installments paid up to N (ADMIN only)")

    @PostMapping("/{loanId}/mark-paid-up-to/{installmentNumber}")
    public ResponseEntity<LoanResponse> markPaidUpTo(
            @Parameter(description = "Loan ID", example = "1", required = true)
            @PathVariable Long loanId,
            @Parameter(description = "Mark every installment from 1 to this number as PAID",
                    example = "23", required = true)
            @PathVariable int installmentNumber
    ) {
                loanService.markPaidUpTo(loanId, installmentNumber);
                return ResponseEntity.ok(loanService.getLoan(loanId));
    }
}
