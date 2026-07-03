package com.bank.loanengine.domain;

/**
 * Identifies which business strategy was applied to a prepayment or settlement event.
 * This assessment implements Category A (Prepayment of Principal) in full:
 *   - REDUCE_EMI_KEEP_TENOR (Option A)
 *   - REDUCE_TENOR_KEEP_EMI (Option B)
 *   - ADVANCE_INSTALLMENTS (Option C)
 *
 * The Category B (Early Settlement) values are declared here to keep the design open for
 * extension (Open/Closed Principle) but are not wired to a strategy implementation, since the
 * assessment only requires one category to be implemented.
 */
public enum BusinessOption {
    // Category A - Prepayment of Principal
    REDUCE_EMI_KEEP_TENOR,
    REDUCE_TENOR_KEEP_EMI,
    ADVANCE_INSTALLMENTS,

    // Category B - Full Early Settlement (not implemented in this submission)
    TRUE_SETTLEMENT,
    RULE_OF_78,
    DISCOUNTED_SETTLEMENT
}
