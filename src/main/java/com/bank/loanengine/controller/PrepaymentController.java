package com.bank.loanengine.controller;

import com.bank.loanengine.dto.PrepaymentRequest;
import com.bank.loanengine.dto.PrepaymentResponse;
import com.bank.loanengine.exception.ErrorResponse;
import com.bank.loanengine.service.PrepaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Prepayments",
        description = """
                Applies a partial prepayment of principal to a loan using one of three
                business strategies (Category A of the assessment):
                
                | Option | Strategy | Effect |
                |--------|----------|--------|
                | **A** | `REDUCE_EMI_KEEP_TENOR` | EMI recalculated downward; tenor unchanged |
                | **B** | `REDUCE_TENOR_KEEP_EMI` | Tenor shortened; EMI unchanged |
                | **C** | `ADVANCE_INSTALLMENTS` | Pool of future EMIs pre-paid; no recalculation |
                """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/loans/{loanId}/prepayments")
public class PrepaymentController {

    private final PrepaymentService prepaymentService;

    public PrepaymentController(PrepaymentService prepaymentService) {
        this.prepaymentService = prepaymentService;
    }

    @Operation(
            summary = "Apply a partial prepayment of principal (ADMIN only)",
            description = """
                    Processes a lump-sum partial prepayment at the given installment number and
                    restructures the loan schedule according to the chosen business option.
                    
                    ---
                    
                    ### Assessment scenario
                    
                    **Setup:** create a loan with principal 1,000,000 · 12% p.a. · 60 months,
                    then call `POST /api/v1/loans/{id}/mark-paid-up-to/23`.
                    
                    **Option A — Reduce EMI, Keep Tenor:**
                    ```json
                    { "installmentNumber": 24, "amount": 200000, "option": "REDUCE_EMI_KEEP_TENOR" }
                    ```
                    → outstanding ≈ **480,000** · tenor stays **36 remaining months** · new EMI ≈ **15,700**
                    
                    **Option B — Reduce Tenor, Keep EMI:**
                    ```json
                    { "installmentNumber": 24, "amount": 200000, "option": "REDUCE_TENOR_KEEP_EMI" }
                    ```
                    → outstanding ≈ **480,000** · EMI stays ≈ **22,244** · remaining tenor ≈ **22 months**
                    
                    **Option C — Advance Installments (no recalculation):**
                    ```json
                    { "installmentNumber": 24, "amount": 200000, "option": "ADVANCE_INSTALLMENTS" }
                    ```
                    → principal / EMI / tenor **unchanged** · ≈ **9 future installments** marked `ADVANCED`
                    
                    ---
                    
                    ### Side effects
                    - The loan's amortisation schedule is mutated and persisted atomically.
                    - The loan is evicted from the Redis cache.
                    - A `loan.prepayment.applied` event is published to Kafka.
                    - An immutable row is written to the `loan_transaction` table.
                    
                    **Required role:** `ROLE_ADMIN`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prepayment applied — updated loan returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PrepaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (negative amount, missing fields)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan or installment not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Business rule violation — amount exceeds outstanding principal, or prepayment at final installment",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<PrepaymentResponse> processPrepayment(
            @Parameter(description = "Loan ID", example = "1", required = true)
            @PathVariable Long loanId,
            @Valid @RequestBody PrepaymentRequest request
    ) {
        return ResponseEntity.ok(prepaymentService.processPrepayment(loanId, request));
    }
}
