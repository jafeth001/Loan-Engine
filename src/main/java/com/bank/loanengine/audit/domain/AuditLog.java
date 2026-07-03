package com.bank.loanengine.audit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable audit record created every time the {@code LoanEventConsumer} successfully
 * processes a Kafka message.  Captures the full event payload as JSON alongside the Kafka
 * delivery metadata (topic, partition, offset, consumer group) so that messages can be
 * replayed or investigated without querying the broker.
 *
 * <p><strong>Immutability contract:</strong> rows are INSERT-only.  No UPDATE or DELETE
 * operations should ever be issued against this table — it is a tamper-evident ledger.
 */
@Entity
@Table(
        name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_loan_id",    columnList = "loan_id"),
                @Index(name = "idx_audit_event_type", columnList = "event_type"),
                @Index(name = "idx_audit_processed_at", columnList = "processed_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The {@code eventId} UUID embedded in the domain event — allows idempotency checks. */
    @Column(name = "event_id", nullable = false, length = 36, unique = true)
    private String eventId;

    /** The loan this event relates to. Nullable in case future events are not loan-specific. */
    @Column(name = "loan_id")
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AuditEventType eventType;

    /** Full event payload serialised as JSON for complete traceability. */
    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "kafka_topic", nullable = false, length = 100)
    private String kafkaTopic;

    @Column(name = "kafka_partition", nullable = false)
    private Integer kafkaPartition;

    @Column(name = "kafka_offset", nullable = false)
    private Long kafkaOffset;

    @Column(name = "kafka_consumer_group", nullable = false, length = 100)
    private String kafkaConsumerGroup;

    /**
     * Wall-clock time at which the consumer processed the message.
     */
    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    /** Non-null when processing failed and the record was written as a dead-letter entry. */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private AuditStatus status = AuditStatus.SUCCESS;
}
