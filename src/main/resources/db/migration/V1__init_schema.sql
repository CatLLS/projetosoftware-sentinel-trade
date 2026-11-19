-- <<critical>> {secrecy}: password_hash (BCrypt) and mfa_secret (AES-GCM encrypted)
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    mfa_secret    VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL
);

-- <<critical>> {integrity}: balances can never go negative; @Version guards concurrent updates
CREATE TABLE accounts (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT        NOT NULL UNIQUE,
    available_balance DECIMAL(19,4) NOT NULL,
    reserved_balance  DECIMAL(19,4) NOT NULL,
    daily_risk_limit  DECIMAL(19,4) NOT NULL,
    version           BIGINT        NOT NULL,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_accounts_balances CHECK (available_balance >= 0 AND reserved_balance >= 0)
);

CREATE TABLE positions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id        BIGINT        NOT NULL,
    asset_symbol      VARCHAR(12)   NOT NULL,
    quantity          DECIMAL(19,6) NOT NULL,
    reserved_quantity DECIMAL(19,6) NOT NULL,
    average_price     DECIMAL(19,4) NOT NULL,
    version           BIGINT        NOT NULL,
    CONSTRAINT fk_positions_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT uk_positions_account_asset UNIQUE (account_id, asset_symbol),
    CONSTRAINT chk_positions_quantities CHECK (reserved_quantity >= 0 AND reserved_quantity <= quantity)
);

-- price = limit price (LIMIT) or reference market price used for the reservation (MARKET)
CREATE TABLE orders (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key   VARCHAR(64)   NOT NULL UNIQUE,
    account_id        BIGINT        NOT NULL,
    order_type        VARCHAR(10)   NOT NULL,
    asset_symbol      VARCHAR(12)   NOT NULL,
    side              VARCHAR(4)    NOT NULL,
    quantity          DECIMAL(19,6) NOT NULL,
    executed_quantity DECIMAL(19,6) NOT NULL,
    price             DECIMAL(19,4) NOT NULL,
    status            VARCHAR(30)   NOT NULL,
    rejection_reason  VARCHAR(255),
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    version           BIGINT        NOT NULL,
    CONSTRAINT fk_orders_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT chk_orders_quantities CHECK (quantity > 0 AND executed_quantity >= 0
                                            AND executed_quantity <= quantity AND price > 0),
    INDEX idx_orders_account_created (account_id, created_at),
    INDEX idx_orders_status_updated (status, updated_at)
);

-- <<audit>>: append-only trail
CREATE TABLE audit_logs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type     VARCHAR(50) NOT NULL,
    actor_id       BIGINT,
    entity_name    VARCHAR(50),
    entity_id      VARCHAR(64),
    previous_state VARCHAR(30),
    new_state      VARCHAR(30),
    metadata_json  JSON,
    ip_address     VARCHAR(45),
    created_at     DATETIME(6) NOT NULL,
    INDEX idx_audit_entity (entity_name, entity_id)
);

CREATE TRIGGER audit_logs_no_update BEFORE UPDATE ON audit_logs
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs is append-only';

CREATE TRIGGER audit_logs_no_delete BEFORE DELETE ON audit_logs
    FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs is append-only';
