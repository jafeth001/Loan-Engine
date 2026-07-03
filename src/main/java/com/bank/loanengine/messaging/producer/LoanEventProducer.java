package com.bank.loanengine.messaging.producer;

import com.bank.loanengine.messaging.event.LoanCreatedEvent;
import com.bank.loanengine.messaging.event.PrepaymentAppliedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes loan domain events to Kafka. Send errors are logged but never propagated to the
 * caller: Kafka is treated as a best-effort event bus; it must not roll back a database
 * transaction that has already committed. For stricter at-least-once delivery the producer
 * could be wrapped in a transactional outbox pattern.
 */
@Component
public class LoanEventProducer {

    public static final String TOPIC_LOAN_CREATED          = "loan.created";
    public static final String TOPIC_PREPAYMENT_APPLIED    = "loan.prepayment.applied";

    private static final Logger log = LoggerFactory.getLogger(LoanEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LoanEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a {@link LoanCreatedEvent}. The message key is the string form of the loan id
     * so that all events for one loan land in the same partition (ordering guarantee).
     */
    public void publishLoanCreated(LoanCreatedEvent event) {
        send(TOPIC_LOAN_CREATED, String.valueOf(event.loanId()), event);
    }

    /**
     * Publishes a {@link PrepaymentAppliedEvent}.
     */
    public void publishPrepaymentApplied(PrepaymentAppliedEvent event) {
        send(TOPIC_PREPAYMENT_APPLIED, String.valueOf(event.loanId()), event);
    }

    private void send(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic [{}] for key [{}]: {}", topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Published event to topic [{}] partition [{}] offset [{}] key [{}]",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        key);
            }
        });
    }
}
