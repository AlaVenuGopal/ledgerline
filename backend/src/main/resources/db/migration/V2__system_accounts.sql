-- V2__system_accounts.sql
-- Seed the System Treasury Reserve Account for Double-Entry Balanced Movements

INSERT INTO accounts (account_number, user_id, account_type, currency, balance_paise, status, created_at, updated_at)
VALUES ('LL-SYS-TREASURY-01', NULL, 'SYSTEM_TREASURY', 'INR', 100000000000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

