package com.bank.loanengine.audit.domain;

/**
 * Identifies the type of domain event that produced an {@link AuditLog} row.
 */
public enum AuditEventType {
    LOAN_CREATED,
    PREPAYMENT_APPLIED
}
