package com.bank.loanengine.messaging.event;

import com.bank.loanengine.domain.BusinessOption;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain event published to the {@code loan.prepayment.applied} Kafka topic every time a
 * partial prepayment is successfully processed. Contains enough context for downstream
 * consumers (e.g. notifications, risk/reporting) to act without querying the loan service.
 */
public record PrepaymentAppliedEvent(

        String eventId,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        LocalDateTime occurredAt,

        Long loanId,
        BusinessOption optionApplied,
        Integer installmentNumber,
        BigDecimal prepaymentAmount,
        BigDecimal outstandingPrincipalBefore,
        BigDecimal outstandingPrincipalAfter,
        BigDecimal newEmiAmount,
        Integer newRemainingTenorMonths,
        String details
) {
}
