package com.bank.loanengine.messaging.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain event published to the {@code loan.created} Kafka topic every time a new loan is
 * created and its amortisation schedule has been persisted. Downstream consumers (audit,
 * notifications, analytics) can subscribe to this topic without coupling to the loan service.
 */
public record LoanCreatedEvent(

        String eventId,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        LocalDateTime occurredAt,

        Long loanId,
        BigDecimal principalAmount,
        BigDecimal annualInterestRate,
        Integer tenorMonths,
        BigDecimal emiAmount,
        LocalDate startDate
) {
}
