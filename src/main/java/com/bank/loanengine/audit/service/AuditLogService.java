package com.bank.loanengine.audit.service;

import com.bank.loanengine.audit.domain.AuditEventType;
import com.bank.loanengine.audit.domain.AuditLog;
import com.bank.loanengine.audit.domain.AuditStatus;
import com.bank.loanengine.audit.repository.AuditLogRepository;
import com.bank.loanengine.exception.LoanNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;


    // ── Write side ───────────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog recordSuccess(
            String eventId,
            Long loanId,
            AuditEventType eventType,
            String payloadJson,
            String kafkaTopic,
            int kafkaPartition,
            long kafkaOffset,
            String consumerGroup
    ) {
        if (auditLogRepository.existsByEventId(eventId)) {
            log.warn("Duplicate event skipped — eventId={} already audited", eventId);
            return auditLogRepository.findByEventId(eventId).orElseThrow();
        }

        AuditLog entry = AuditLog.builder()
                .eventId(eventId)
                .loanId(loanId)
                .eventType(eventType)
                .payload(payloadJson)
                .kafkaTopic(kafkaTopic)
                .kafkaPartition(kafkaPartition)
                .kafkaOffset(kafkaOffset)
                .kafkaConsumerGroup(consumerGroup)
                .status(AuditStatus.SUCCESS)
                .build();

        AuditLog saved = auditLogRepository.save(entry);
        log.debug("Audit saved — id={} eventId={} type={} loanId={}", saved.getId(), eventId, eventType, loanId);
        return saved;
    }

    /**
     * Persists a failed audit entry. Errors during consumer processing are always recorded
     * so that failures are visible in the audit trail without needing to check Kafka DLQs.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog recordFailure(
            String eventId,
            Long loanId,
            AuditEventType eventType,
            String payloadJson,
            String kafkaTopic,
            int kafkaPartition,
            long kafkaOffset,
            String consumerGroup,
            String errorMessage
    ) {
        AuditLog entry = AuditLog.builder()
                .eventId(eventId)
                .loanId(loanId)
                .eventType(eventType)
                .payload(payloadJson)
                .kafkaTopic(kafkaTopic)
                .kafkaPartition(kafkaPartition)
                .kafkaOffset(kafkaOffset)
                .kafkaConsumerGroup(consumerGroup)
                .status(AuditStatus.FAILED)
                .errorMessage(errorMessage)
                .build();

        AuditLog saved = auditLogRepository.save(entry);
        log.error("Audit FAILURE recorded — id={} eventId={} type={} loanId={} error={}",
                saved.getId(), eventId, eventType, loanId, errorMessage);
        return saved;
    }

    // ── Read side ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }


    @Transactional(readOnly = true)
    public Page<AuditLog> findByLoanId(Long loanId, Pageable pageable) {
        return auditLogRepository.findByLoanIdOrderByProcessedAtDesc(loanId, pageable);
    }

}
