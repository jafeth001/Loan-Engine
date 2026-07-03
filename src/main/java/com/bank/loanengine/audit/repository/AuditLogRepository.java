package com.bank.loanengine.audit.repository;

import com.bank.loanengine.audit.domain.AuditEventType;
import com.bank.loanengine.audit.domain.AuditLog;
import com.bank.loanengine.audit.domain.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** All audits for a specific loan, newest first. */
    Page<AuditLog> findByLoanIdOrderByProcessedAtDesc(Long loanId, Pageable pageable);

    /** All audits of a given event type. */
    Page<AuditLog> findByEventTypeOrderByProcessedAtDesc(AuditEventType eventType, Pageable pageable);

    /** All audits for a loan filtered by event type. */
    Page<AuditLog> findByLoanIdAndEventTypeOrderByProcessedAtDesc(
            Long loanId, AuditEventType eventType, Pageable pageable);

    /** Idempotency guard — check if we have already processed a given eventId. */
    boolean existsByEventId(String eventId);

    /** Look up a specific audit entry by the domain event's own UUID. */
    Optional<AuditLog> findByEventId(String eventId);

    /** Filter by status (SUCCESS / FAILED). */
    Page<AuditLog> findByStatusOrderByProcessedAtDesc(AuditStatus status, Pageable pageable);

    /** Count audit entries per event type for dashboard/summary. */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a GROUP BY a.eventType")
    java.util.List<Object[]> countByEventType();

    /** All audits in a time window — useful for compliance reports. */
    @Query("SELECT a FROM AuditLog a WHERE a.processedAt BETWEEN :from AND :to ORDER BY a.processedAt DESC")
    Page<AuditLog> findByProcessedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to,
            Pageable pageable);
}
