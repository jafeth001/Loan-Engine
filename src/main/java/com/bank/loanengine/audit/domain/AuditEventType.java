package com.bank.loanengine.audit.domain;

/**
 * Identifies the type of domain event that produced an {@link AuditLog} row.
 * One enum value per Kafka topic so that audit queries can filter by event type.
 */
public enum AuditEventType {
    LOAN_CREATED,
    PREPAYMENT_APPLIED
}
