package com.bank.loanengine.messaging.consumer;

import com.bank.loanengine.audit.domain.AuditEventType;
import com.bank.loanengine.audit.service.AuditLogService;
import com.bank.loanengine.messaging.event.LoanCreatedEvent;
import com.bank.loanengine.messaging.event.PrepaymentAppliedEvent;
import com.bank.loanengine.messaging.producer.LoanEventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes loan domain events from Kafka and persists an immutable {@code AuditLog} row in the
 * database for every message processed.
 *
 * <h3>Reliability guarantees</h3>
 * <ul>
 *   <li>Each audit write runs in its own {@code REQUIRES_NEW} transaction so that a failure
 *       in the broader consumer context never silently drops the audit record.</li>
 *   <li>Idempotency: duplicate deliveries (Kafka at-least-once) are detected by
 *       {@code eventId} and silently skipped.</li>
 *   <li>Consumer exceptions are caught, logged, and recorded as {@code FAILED} audit entries
 *       rather than propagating — this prevents Kafka from re-delivering the message
 *       indefinitely and avoids blocking the partition.</li>
 * </ul>
 */
@Component
public class LoanEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LoanEventConsumer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final AuditLogService auditLogService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    public LoanEventConsumer(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    // ── loan.created ─────────────────────────────────────────────────────────

    @KafkaListener(
            topics = LoanEventProducer.TOPIC_LOAN_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onLoanCreated(
            ConsumerRecord<String, LoanCreatedEvent> record,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        LoanCreatedEvent event = record.value();

        log.info("[CONSUMER] LOAN_CREATED | eventId={} | loanId={} | principal={} | rate={}% "
                        + "| tenor={}mo | emi={} | partition={} offset={}",
                event.eventId(), event.loanId(), event.principalAmount(),
                event.annualInterestRate(), event.tenorMonths(), event.emiAmount(),
                partition, offset);

        try {
            String payload = toJson(event);
            auditLogService.recordSuccess(
                    event.eventId(),
                    event.loanId(),
                    AuditEventType.LOAN_CREATED,
                    payload,
                    record.topic(),
                    partition,
                    offset,
                    consumerGroupId
            );
        } catch (Exception ex) {
            log.error("[CONSUMER] Failed to process LOAN_CREATED eventId={} — recording failure audit",
                    event.eventId(), ex);
            auditLogService.recordFailure(
                    event.eventId(),
                    event.loanId(),
                    AuditEventType.LOAN_CREATED,
                    safeToJson(event),
                    record.topic(),
                    partition,
                    offset,
                    consumerGroupId,
                    ex.getMessage()
            );
        }
    }

    // ── loan.prepayment.applied ───────────────────────────────────────────────

    @KafkaListener(
            topics = LoanEventProducer.TOPIC_PREPAYMENT_APPLIED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPrepaymentApplied(
            ConsumerRecord<String, PrepaymentAppliedEvent> record,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        PrepaymentAppliedEvent event = record.value();

        log.info("[CONSUMER] PREPAYMENT_APPLIED | eventId={} | loanId={} | option={} "
                        + "| installment={} | amount={} | principalBefore={} | principalAfter={} "
                        + "| newEmi={} | newTenor={}mo | partition={} offset={}",
                event.eventId(), event.loanId(), event.optionApplied(),
                event.installmentNumber(), event.prepaymentAmount(),
                event.outstandingPrincipalBefore(), event.outstandingPrincipalAfter(),
                event.newEmiAmount(), event.newRemainingTenorMonths(),
                partition, offset);

        try {
            String payload = toJson(event);
            auditLogService.recordSuccess(
                    event.eventId(),
                    event.loanId(),
                    AuditEventType.PREPAYMENT_APPLIED,
                    payload,
                    record.topic(),
                    partition,
                    offset,
                    consumerGroupId
            );
        } catch (Exception ex) {
            log.error("[CONSUMER] Failed to process PREPAYMENT_APPLIED eventId={} — recording failure audit",
                    event.eventId(), ex);
            auditLogService.recordFailure(
                    event.eventId(),
                    event.loanId(),
                    AuditEventType.PREPAYMENT_APPLIED,
                    safeToJson(event),
                    record.topic(),
                    partition,
                    offset,
                    consumerGroupId,
                    ex.getMessage()
            );
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String toJson(Object obj) throws JsonProcessingException {
        return MAPPER.writeValueAsString(obj);
    }

    /** Fallback serialization that never throws — used in the error path. */
    private String safeToJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"payload serialization failed\"}";
        }
    }
}
