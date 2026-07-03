package com.bank.loanengine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Parameters for creating a new loan and generating its amortisation schedule")
public record CreateLoanRequest(

        @Schema(description = "Loan principal amount", example = "1000000.00", minimum = "0.01")
        @NotNull(message = "principalAmount is required")
        @DecimalMin(value = "0.01", message = "principalAmount must be positive")
        BigDecimal principalAmount,

        @Schema(description = "Annual interest rate as a percentage (e.g. 12 for 12% p.a.)",
                example = "12.00", minimum = "0.0")
        @NotNull(message = "annualInterestRate is required")
        @DecimalMin(value = "0.0", message = "annualInterestRate cannot be negative")
        BigDecimal annualInterestRate,

        @Schema(description = "Loan duration in months", example = "60", minimum = "1")
        @NotNull(message = "tenorMonths is required")
        @Min(value = 1, message = "tenorMonths must be at least 1")
        Integer tenorMonths,

        @Schema(description = "Loan start date. Defaults to today if omitted. "
                + "Installment 1 is due one month after this date.",
                example = "2024-01-01")
        LocalDate startDate
) {}
