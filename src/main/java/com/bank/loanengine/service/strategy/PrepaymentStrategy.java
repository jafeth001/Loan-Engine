package com.bank.loanengine.service.strategy;

import com.bank.loanengine.domain.BusinessOption;

/**
 * Strategy interface for handling a partial-prepayment business option (Category A:
 * Options A, B, C). Each concrete implementation owns the full logic for recalculating (or
 * deliberately not recalculating) the loan schedule, keeping the orchestrating service free of
 * large conditional blocks and making it trivial to add new options later (Open/Closed
 * Principle).
 */
public interface PrepaymentStrategy {

    /** The business option this strategy implements. */
    BusinessOption supportedOption();

    /**
     * Applies the prepayment to the loan's schedule (mutating the in-memory entity graph held in
     * {@code context.loan()}) and returns a summary of the outcome. Persistence is the
     * responsibility of the calling service.
     */
    PrepaymentResult apply(PrepaymentContext context);
}
