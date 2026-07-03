package com.bank.loanengine.controller;

import com.bank.loanengine.dto.PrepaymentRequest;
import com.bank.loanengine.dto.PrepaymentResponse;
import com.bank.loanengine.service.PrepaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @Operation(summary = "Apply a partial prepayment of principal (ADMIN only)")
    @PostMapping
    public ResponseEntity<PrepaymentResponse> processPrepayment(
            @Parameter(description = "Loan ID", example = "1", required = true)
            @PathVariable Long loanId,
            @Valid @RequestBody PrepaymentRequest request
    ) {
        return ResponseEntity.ok(prepaymentService.processPrepayment(loanId, request));
    }
}
