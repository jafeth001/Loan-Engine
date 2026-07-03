package com.bank.loanengine.auth.domain;

/**
 * Application roles.
 * <ul>
 *   <li>ADMIN  — full access to all endpoints; can create, query and prepay any loan.</li>
 *   <li>CUSTOMER — read-only access restricted to their own loans.
 *                  POST (create loan, prepayment) is blocked for CUSTOMER in SecurityConfig.</li>
 * </ul>
 */
public enum Role {
    ROLE_ADMIN,
    ROLE_CUSTOMER
}
