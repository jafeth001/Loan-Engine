package com.bank.loanengine.controller;

import com.bank.loanengine.domain.Loan;
import com.bank.loanengine.dto.CreateLoanRequest;
import com.bank.loanengine.dto.LoanResponse;
import com.bank.loanengine.exception.ErrorResponse;
import com.bank.loanengine.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Create a loan (ADMIN only)",
            description = """
                    Creates a new loan record and generates the full month-by-month reducing-balance
                    amortisation schedule.
                    
                    **Assessment base loan:**
                    ```json
                    {
                      "principalAmount": 1000000,
                      "annualInterestRate": 12,
                      "tenorMonths": 60,
                      "startDate": "2024-01-01"
                    }
                    ```
                    Produces 60 installments with EMI ≈ 22,244.  
                    Outstanding principal after installment 24 ≈ 680,000.
                    
                    A `loan.created` event is published to Kafka after a successful save.  
                    **Required role:** `ROLE_ADMIN`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan created with full schedule",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role (ROLE_CUSTOMER cannot create loans)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        Loan loan = loanService.createLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.from(loan));
    }

    // ── Get loan ──────────────────────────────────────────────────────────────

    @Operation(
            summary = "Get a loan with its full schedule",
            description = """
                    Returns the loan contract details and its complete amortisation schedule.  
                    The response is cached in Redis (TTL 10 min) to reduce database load.  
                    **Required role:** `ROLE_ADMIN` or `ROLE_CUSTOMER`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
        @GetMapping("/{loanId}")
        public ResponseEntity<LoanResponse> getLoan(
            @Parameter(description = "Loan ID", example = "1", required = true)
            @PathVariable Long loanId
    ) {
                return ResponseEntity.ok(loanService.getLoan(loanId));
    }

    // ── Get schedule ──────────────────────────────────────────────────────────

    @Operation(
            summary = "Get the amortisation schedule for a loan",
            description = """
                    Alias of `GET /api/v1/loans/{loanId}` — returns the same payload.  
                    Useful when a client only wants the installment table without storing the
                    full loan object on a separate route.  
                    **Required role:** `ROLE_ADMIN` or `ROLE_CUSTOMER`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{loanId}/schedule")
    public ResponseEntity<LoanResponse> getSchedule(
            @Parameter(description = "Loan ID", example = "1", required = true)
            @PathVariable Long loanId
    ) {
                return ResponseEntity.ok(loanService.getLoan(loanId));
    }

    // ── Mark paid up to ───────────────────────────────────────────────────────

    @Operation(
            summary = "Mark installments 1–N as PAID (ADMIN only)",
            description = """
                    Convenience endpoint that simulates the assessment's stated assumption:
                    *"all installments prior to the selected installment have already been paid."*
                    
                    Call this before applying a prepayment to mark the preceding installments as `PAID`.
                    
                    **Typical usage for the assessment scenario:**
                    ```
                    POST /api/v1/loans/1/mark-paid-up-to/23
                    ```
                    Then apply a prepayment at installment 24.
                    
                    This operation evicts the loan from the Redis cache.  
                    **Required role:** `ROLE_ADMIN`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Installments marked as PAID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
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
