package com.bank.loanengine.audit.domain;

/**
 * Processing outcome for an audit log entry.
 * <ul>
 *   <li>{@code SUCCESS} — the event was fully processed and persisted without error.</li>
 *   <li>{@code FAILED}  — the consumer caught an exception; the error detail is stored in
 *                         {@link AuditLog#getErrorMessage()} for investigation.</li>
 * </ul>
 */
public enum AuditStatus {
    SUCCESS,
    FAILED
}
