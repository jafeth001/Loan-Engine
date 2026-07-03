-- ============================================================================
-- H2 schema (used for tests and for running the app locally without MySQL)
-- Functionally equivalent to schema.sql but trimmed of MySQL-only syntax.
-- ============================================================================

CREATE TABLE IF NOT EXISTS loan (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    principal_amount    DECIMAL(18,2) NOT NULL,
    annual_interest_rate DECIMAL(7,4) NOT NULL,
    tenor_months        INT NOT NULL,
    emi_amount          DECIMAL(18,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date          DATE NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedule_loan FOREIGN KEY (loan_id) REFERENCES loan(id) ON DELETE CASCADE,
    CONSTRAINT uq_loan_installment UNIQUE (loan_id, installment_number)
);

CREATE INDEX IF NOT EXISTS idx_schedule_loan_id ON loan_schedule_installment (loan_id);

CREATE TABLE IF NOT EXISTS loan_transaction (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id             BIGINT NOT NULL,
    transaction_type    VARCHAR(30) NOT NULL,
    business_option     VARCHAR(40),
    amount              DECIMAL(18,2) NOT NULL,
    installment_number  INT,
    balance_before       DECIMAL(18,2),
    balance_after        DECIMAL(18,2),
    transaction_date     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes                VARCHAR(500),
    CONSTRAINT fk_transaction_loan FOREIGN KEY (loan_id) REFERENCES loan(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_transaction_loan_id ON loan_transaction (loan_id);


-- ============================================================================
-- Users table for JWT authentication (H2 / test)
-- ============================================================================
CREATE TABLE IF NOT EXISTS app_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(80)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_username UNIQUE (username)
);


-- ============================================================================
-- Audit log (H2 / test)
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id             VARCHAR(36)   NOT NULL,
    loan_id              BIGINT,
    event_type           VARCHAR(30)   NOT NULL,
    payload              CLOB          NOT NULL,
    kafka_topic          VARCHAR(100)  NOT NULL,
    kafka_partition      INT           NOT NULL,
    kafka_offset         BIGINT        NOT NULL,
    kafka_consumer_group VARCHAR(100)  NOT NULL,
    status               VARCHAR(10)   NOT NULL DEFAULT 'SUCCESS',
    error_message        VARCHAR(1000),
    processed_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_audit_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_audit_loan_id      ON audit_log (loan_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_type   ON audit_log (event_type);
CREATE INDEX IF NOT EXISTS idx_audit_processed_at ON audit_log (processed_at);
