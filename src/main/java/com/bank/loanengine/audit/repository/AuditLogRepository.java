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

    Page<AuditLog> findByLoanIdOrderByProcessedAtDesc(Long loanId, Pageable pageable);

    boolean existsByEventId(String eventId);

    Optional<AuditLog> findByEventId(String eventId);
}
