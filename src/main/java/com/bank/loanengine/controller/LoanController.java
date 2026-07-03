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
        description = "Create loans, retrieve loan details and amortisation schedules, and mark installments as paid."
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    // ───────────────────────────────────────────────────────────────
    // Create Loan
    // ───────────────────────────────────────────────────────────────

    @Operation(summary = "Create a loan (ADMIN only)")
    @PostMapping
    public ResponseEntity<LoanResponse> createLoan(
            @Valid @RequestBody CreateLoanRequest request) {

        Loan loan = loanService.createLoan(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(LoanResponse.from(loan));
    }

    // ───────────────────────────────────────────────────────────────
    // Get Loan
    // ───────────────────────────────────────────────────────────────

    @Operation(summary = "Get a loan with its schedule")
    @GetMapping("/{loanId}")
    public ResponseEntity<LoanResponse> getLoan(
            @Parameter(description = "Loan ID", example = "1")
            @PathVariable Long loanId) {

        return ResponseEntity.ok(loanService.getLoan(loanId));
    }

    // ───────────────────────────────────────────────────────────────
    // Get Schedule
    // ───────────────────────────────────────────────────────────────

    @Operation(summary = "Get loan amortisation schedule")
    @GetMapping("/{loanId}/schedule")
    public ResponseEntity<LoanResponse> getSchedule(
            @Parameter(description = "Loan ID", example = "1")
            @PathVariable Long loanId) {

        return ResponseEntity.ok(loanService.getLoan(loanId));
    }

    // ───────────────────────────────────────────────────────────────
    // Mark Paid
    // ───────────────────────────────────────────────────────────────

    @Operation(summary = "Mark installments as PAID up to a given installment")
    @PostMapping("/{loanId}/mark-paid-up-to/{installmentNumber}")
    public ResponseEntity<LoanResponse> markPaidUpTo(

            @Parameter(description = "Loan ID", example = "1")
            @PathVariable Long loanId,

            @Parameter(
                    description = "All installments from 1 up to this installment are marked PAID",
                    example = "23")
            @PathVariable int installmentNumber) {

        loanService.markPaidUpTo(loanId, installmentNumber);

        return ResponseEntity.ok(loanService.getLoan(loanId));
    }
}