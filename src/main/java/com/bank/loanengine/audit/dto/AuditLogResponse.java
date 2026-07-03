package com.bank.loanengine.audit.dto;

import com.bank.loanengine.audit.domain.AuditEventType;
import com.bank.loanengine.audit.domain.AuditLog;
import com.bank.loanengine.audit.domain.AuditStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "A single audit log entry capturing a Kafka event consumed by the system")
public record AuditLogResponse(

        @Schema(description = "Auto-generated database ID of this audit entry", example = "1")
        Long id,

        @Schema(description = "UUID set by the event producer — used for idempotency checks",
                example = "a3f8c2d1-4b7e-4f3a-9c1d-2e5b8a0f6d4c")
        String eventId,

        @Schema(description = "Loan ID this event relates to", example = "1")
        Long loanId,

        @Schema(description = "Type of domain event", example = "LOAN_CREATED")
        AuditEventType eventType,

        @Schema(description = "Full event payload as JSON")
        JsonNode payload,

        @Schema(description = "Kafka topic the event was consumed from", example = "loan.created")
        String kafkaTopic,

        @Schema(description = "Kafka partition number", example = "0")
        Integer kafkaPartition,

        @Schema(description = "Kafka offset within the partition", example = "42")
        Long kafkaOffset,

        @Schema(description = "Consumer group that processed this message",
                example = "loan-engine-group")
        String kafkaConsumerGroup,

        @Schema(description = "Processing outcome", example = "SUCCESS")
        AuditStatus status,

        @Schema(description = "Error detail when status is FAILED, null otherwise", nullable = true)
        String errorMessage,

        @Schema(description = "Wall-clock time at which the consumer processed this event",
                example = "2024-06-01T12:05:00")
        LocalDateTime processedAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    public static AuditLogResponse from(AuditLog log) {
        JsonNode payloadNode;
        try {
            payloadNode = MAPPER.readTree(log.getPayload());
        } catch (Exception e) {
            payloadNode = MAPPER.getNodeFactory().textNode(log.getPayload());
        }

        return new AuditLogResponse(
                log.getId(),
                log.getEventId(),
                log.getLoanId(),
                log.getEventType(),
                payloadNode,
                log.getKafkaTopic(),
                log.getKafkaPartition(),
                log.getKafkaOffset(),
                log.getKafkaConsumerGroup(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getProcessedAt()
        );
    }
}
