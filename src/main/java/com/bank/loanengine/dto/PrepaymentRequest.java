package com.bank.loanengine.dto;

import com.bank.loanengine.domain.BusinessOption;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Parameters for processing a partial prepayment of principal")
public record PrepaymentRequest(

        @Schema(
                description = "The installment number at which the prepayment event occurs. "
                        + "All earlier installments are assumed already paid. "
                        + "Must be ≥ 1 and < the loan's final installment.",
                example = "24",
                minimum = "1"
        )
        @NotNull(message = "installmentNumber is required")
        @Min(value = 1, message = "installmentNumber must be at least 1")
        Integer installmentNumber,

        @Schema(
                description = "Cash amount the customer is prepaying. "
                        + "Must be positive and must not exceed the outstanding principal "
                        + "at the given installment.",
                example = "200000.00",
                minimum = "0.01"
        )
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount,

        @Schema(
                description = """
                        Business strategy to apply:
                        - `REDUCE_EMI_KEEP_TENOR` **(Option A)** — principal drops; remaining tenor stays fixed; EMI is recalculated downward.
                        - `REDUCE_TENOR_KEEP_EMI` **(Option B)** — principal drops; EMI stays fixed; remaining tenor is shortened.
                        - `ADVANCE_INSTALLMENTS` **(Option C)** — prepayment is treated as a pool of future EMIs paid in advance; no recalculation.
                        """,
                example = "REDUCE_EMI_KEEP_TENOR",
                allowableValues = {"REDUCE_EMI_KEEP_TENOR", "REDUCE_TENOR_KEEP_EMI", "ADVANCE_INSTALLMENTS"}
        )
        @NotNull(message = "option is required")
        BusinessOption option
) {}
