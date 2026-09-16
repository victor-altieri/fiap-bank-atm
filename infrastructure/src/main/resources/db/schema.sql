CREATE TABLE IF NOT EXISTS tb_account (
    id VARCHAR(36) PRIMARY KEY,
    agency VARCHAR(10) NOT NULL,
    number VARCHAR(20) NOT NULL UNIQUE,
    balance DECIMAL(15, 2) NOT NULL CHECK(balance >= 0),
    status VARCHAR(20) NOT NULL CHECK(status IN ('ACTIVE', 'BLOCKED')),
    pin VARCHAR(4) NOT NULL,
    daily_limit DECIMAL(15, 2) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS tb_transaction (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL CHECK(amount > 0),
    created_at TIMESTAMP NOT NULL,
    description VARCHAR(255) NOT NULL,
    FOREIGN KEY (account_id) REFERENCES tb_account(id)
);
CREATE INDEX IF NOT EXISTS ix_transaction_account_date ON tb_transaction(account_id, created_at);
