-- ============================================================================
-- Loan Settlement & Prepayment Engine — MySQL schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS loan (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    principal_amount    DECIMAL(18,2) NOT NULL,
    annual_interest_rate DECIMAL(7,4) NOT NULL,           -- e.g. 12.0000 for 12% p.a.
    tenor_months        INT NOT NULL,
    emi_amount          DECIMAL(18,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SETTLED, CLOSED
    start_date          DATE NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS loan_schedule_installment (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id             BIGINT NOT NULL,
    installment_number  INT NOT NULL,
    due_date            DATE NOT NULL,
    opening_balance     DECIMAL(18,2) NOT NULL,
    emi_amount          DECIMAL(18,2) NOT NULL,
    principal_component DECIMAL(18,2) NOT NULL,
    interest_component  DECIMAL(18,2) NOT NULL,
    closing_balance     DECIMAL(18,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, ADJUSTED, ADVANCED, CANCELLED
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedule_loan FOREIGN KEY (loan_id) REFERENCES loan(id) ON DELETE CASCADE,
    CONSTRAINT uq_loan_installment UNIQUE (loan_id, installment_number),
    INDEX idx_schedule_loan_id (loan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS loan_transaction (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id             BIGINT NOT NULL,
    transaction_type    VARCHAR(30) NOT NULL,   -- EMI_PAYMENT, PREPAYMENT, SETTLEMENT
    business_option     VARCHAR(40),            -- REDUCE_EMI_KEEP_TENOR, REDUCE_TENOR_KEEP_EMI, ADVANCE_INSTALLMENTS, TRUE_SETTLEMENT, RULE_OF_78, DISCOUNTED_SETTLEMENT
    amount              DECIMAL(18,2) NOT NULL,
    installment_number  INT,                    -- installment at which the event occurred
    balance_before       DECIMAL(18,2),
    balance_after        DECIMAL(18,2),
    transaction_date     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes                VARCHAR(500),
    CONSTRAINT fk_transaction_loan FOREIGN KEY (loan_id) REFERENCES loan(id) ON DELETE CASCADE,
    INDEX idx_transaction_loan_id (loan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================================
-- Users table for JWT authentication
-- ============================================================================
CREATE TABLE IF NOT EXISTS app_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(80)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================================
-- Audit log — immutable ledger of every Kafka event consumed
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id             VARCHAR(36)   NOT NULL,
    loan_id              BIGINT,
    event_type           VARCHAR(30)   NOT NULL,
    payload              TEXT          NOT NULL,
    kafka_topic          VARCHAR(100)  NOT NULL,
    kafka_partition      INT           NOT NULL,
    kafka_offset         BIGINT        NOT NULL,
    kafka_consumer_group VARCHAR(100)  NOT NULL,
    status               VARCHAR(10)   NOT NULL DEFAULT 'SUCCESS',
    error_message        VARCHAR(1000),
    processed_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_audit_event_id UNIQUE (event_id),
    INDEX idx_audit_loan_id (loan_id),
    INDEX idx_audit_event_type (event_type),
    INDEX idx_audit_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
