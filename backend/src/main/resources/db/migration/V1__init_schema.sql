-- V1__init_schema.sql
-- LedgerLine: Core Schema with Double-Entry Ledger, Idempotency, and Audit Trail

-- 1. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('ROLE_ADMINISTRATOR', 'ROLE_ACCOUNT_HOLDER', 'ROLE_AUDITOR')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Accounts Table
-- Crucial Invariant: balance_paise must NEVER be negative
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    user_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    account_type VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' CHECK (account_type IN ('CUSTOMER', 'SYSTEM_TREASURY')),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    balance_paise BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_account_balance_non_negative CHECK (balance_paise >= 0)
);

-- 3. Transactions Table
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    transaction_ref VARCHAR(40) NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) UNIQUE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'REJECTED')),
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    source_account_id BIGINT REFERENCES accounts(id),
    destination_account_id BIGINT REFERENCES accounts(id),
    description VARCHAR(255),
    rejection_reason VARCHAR(255),
    initiated_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 4. Ledger Entries Table (Double-Entry Bookkeeping)
-- Every financial movement must have balanced debit and credit entries referencing the transaction
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE RESTRICT,
    account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    balance_after_paise BIGINT NOT NULL CHECK (balance_after_paise >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 5. Idempotency Records Table
-- Prevents duplicate transactions and enforces payload fingerprinting
CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL, -- SHA-256 hash of the request body
    user_id BIGINT NOT NULL REFERENCES users(id),
    response_status INT NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 6. Audit Trail Table (Append-Only)
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    actor_id BIGINT,
    actor_username VARCHAR(50) NOT NULL,
    target_account_id BIGINT,
    target_transaction_id UUID,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILURE')),
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Performance and Integrity Indexes
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_transactions_source_acc ON transactions(source_account_id);
CREATE INDEX idx_transactions_dest_acc ON transactions(destination_account_id);
CREATE INDEX idx_ledger_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_account_id ON ledger_entries(account_id);
CREATE INDEX idx_audit_actor_id ON audit_events(actor_id);
CREATE INDEX idx_audit_created_at ON audit_events(created_at);
