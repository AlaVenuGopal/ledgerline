# LedgerLine — Threat Model & Security Architecture (THREATS.md)

This document provides a comprehensive threat model for **LedgerLine**, in alignment with financial industry security standards and the Snovasys Fresher Hiring Challenge 2026 requirements.

---

## Threat Matrix Summary

| ID | Threat Category | Primary Mitigation | Verification Test |
|---|---|---|---|
| **THREAT-01** | Unauthorised Account Access | Spring Security Stateless JWT Filter with 401 Unauthorized | `AuthenticationSecurityTests#unauthenticatedRequestToProtectedEndpointReturns401` |
| **THREAT-02** | Horizontal Privilege Escalation | Server-side cryptographic ownership verification in `AccountService` | `AccountOwnershipTests#aliceCannotWithdrawFromBobsAccount` |
| **THREAT-03** | Vertical Privilege Escalation | Spring Security RBAC `@PreAuthorize("hasRole(...)")` | `RoleAuthorizationSecurityTests#accountHolderDeniedAdminEndpoint` |
| **THREAT-04** | Duplicate Transaction Requests | Database-enforced Idempotency Key with SHA-256 fingerprinting | `IdempotencyTests#duplicateIdempotencyKeyReplaysResponseWithoutRetransfer` |
| **THREAT-05** | Concurrent Withdrawal Race Conditions | Row-level pessimistic locking (`PESSIMISTIC_WRITE`) + DB CHECK constraint | `ConcurrencyTests#concurrentWithdrawalsNeverOverdraw` |
| **THREAT-06** | Ledger Imbalance | Atomic transaction (`@Transactional`) + Double-Entry invariant | `FinancialEngineTests#depositCreatesBalancedEntries` |
| **THREAT-07** | Negative Balances | PostgreSQL `CHECK (balance_paise >= 0)` constraint | `FinancialEngineTests#withdrawalRejectsOverdraft` |
| **THREAT-08** | SQL Injection | Spring Data JPA parameterised queries & strictly typed JPA criteria | Parameterized queries in `AccountRepository`, `TransactionRepository` |
| **THREAT-09** | Broken Authentication / Password Guessing | BCrypt password hashing (strength 12) | `AuthenticationSecurityTests#invalidPasswordReturns401` |
| **THREAT-10** | Token Theft or Misuse | Cryptographically signed HMAC-SHA256 JWT with expiration | `AuthenticationSecurityTests#validLoginReturnsJwtToken` |
| **THREAT-11** | Sensitive Information Exposure | Sanitized `@RestControllerAdvice` error model (`ApiError`) | `GlobalExceptionHandler` |
| **THREAT-12** | Audit Record Tampering | Append-only database table; zero UPDATE/DELETE endpoints | `AuditorController#getAuditEvents` |
| **THREAT-13** | Denial of Service (DoS) via Payload Size | Jakarta Bean Validation `@Min`, `@Size`, `@NotBlank` on DTOs | DTO Bean Validation |
| **THREAT-14** | Database Failure During Transfer | Spring `@Transactional(isolation = READ_COMMITTED)` rollback | `FinancialEngineTests` |
| **THREAT-15** | Deadlocks on Concurrent Transfers | Deterministic ascending primary-key lock ordering | `ConcurrencyTests#bidirectionalTransfersDoNotDeadlock` |

---

## Detailed Threat Analyses

### THREAT-01: Unauthorised Account Access
1. **Description**: An unauthenticated attacker attempts to query account balances, view transaction history, or trigger financial movements without logging in.
2. **Impact**: Confidential financial data exposure and unauthorized financial operations.
3. **Attack Scenario**: Attacker sends `GET /api/accounts` or `POST /api/transactions/transfer` with empty or forged headers.
4. **Implemented Mitigation**: Spring Security `SecurityFilterChain` enforces `anyRequest().authenticated()` for all `/api/**` endpoints (except `/api/auth/login` and `/api/health`). Requests lacking a valid Bearer token are intercepted by `JwtAuthenticationFilter` and rejected with `401 Unauthorized` via `AuthenticationEntryPoint`.
5. **Automated Test**: `AuthenticationSecurityTests#unauthenticatedRequestToProtectedEndpointReturns401`
6. **Residual Risk / Limitation**: Compromise of user credentials via phishing or keylogger on client devices. Mitigated by user education and future multi-factor authentication (MFA).

---

### THREAT-02: Horizontal Privilege Escalation
1. **Description**: Account Holder Alice attempts to read or execute money movements against Bob's account.
2. **Impact**: Theft of customer funds or unauthorized viewing of confidential financial records.
3. **Attack Scenario**: Alice authenticates successfully as `alice`, then issues `POST /api/transactions/withdraw` with `accountId` set to Bob's account ID (`LL-ACC-BOB-01`).
4. **Implemented Mitigation**: Server-side ownership validation in `AccountService.validateOwnership(Account account, UserPrincipal principal)`:
   ```java
   if (account.getUser() == null || !account.getUser().getId().equals(principal.getId())) {
       throw new UnauthorizedAccountAccessException("You do not have permission to access account: " + account.getAccountNumber());
   }
   ```
   The backend never trusts client-supplied account ownership claims; it retrieves the verified user ID from the cryptographic JWT context.
5. **Automated Test**: `AccountOwnershipTests#aliceCannotWithdrawFromBobsAccount` and `AccountOwnershipTests#aliceCannotTransferOutOfBobsAccount`
6. **Residual Risk / Limitation**: An administrator with system-level override permissions could reassign accounts. Mitigated by comprehensive audit logs for all administrative actions.

---

### THREAT-03: Vertical Privilege Escalation
1. **Description**: An Account Holder attempts to access administrative endpoints (e.g. creating users) or an Auditor attempts to execute money transfers.
2. **Impact**: Compromise of administrative controls or violation of regulatory read-only boundaries.
3. **Attack Scenario**: Alice sends `GET /api/admin/users`, or an Auditor sends `POST /api/transactions/transfer`.
4. **Implemented Mitigation**: Spring Security method security `@EnableMethodSecurity` paired with `@PreAuthorize("hasRole('ADMINISTRATOR')")` on `AdminController` and `@PreAuthorize("hasAnyRole('ACCOUNT_HOLDER', 'ADMINISTRATOR')")` on `TransactionController`. Auditor role is strictly excluded from write operations.
5. **Automated Test**: `RoleAuthorizationSecurityTests#accountHolderDeniedAdminEndpoint` and `RoleAuthorizationSecurityTests#auditorDeniedTransferExecution`
6. **Residual Risk / Limitation**: Misconfiguration in controller annotations. Mitigated by automated role testing in CI.

---

### THREAT-04: Duplicate Transaction Requests
1. **Description**: Network latency or user double-clicking "Submit" sends identical transfer requests twice.
2. **Impact**: Double money movement (e.g. transferring ₹1,000 twice instead of once).
3. **Attack Scenario**: Client submits a transfer request with `Idempotency-Key: IDEM-123`. The network drops the acknowledgement. Client retries with `Idempotency-Key: IDEM-123`.
4. **Implemented Mitigation**: Server-side database table `idempotency_records` with a unique constraint on `idempotency_key` and SHA-256 payload fingerprinting. If the same key is received with identical payload, the backend returns the stored response from the database without executing the financial transfer again. If the key is reused with a different payload, it is rejected with `409 Conflict`.
5. **Automated Test**: `IdempotencyTests#duplicateIdempotencyKeyReplaysResponseWithoutRetransfer` and `IdempotencyTests#keyReusedWithDifferentPayloadThrowsConflict`
6. **Residual Risk / Limitation**: Client fails to supply an idempotency key. Mitigated by automatic client-side UUID generation in the frontend service layer.

---

### THREAT-05: Concurrent Withdrawal Race Conditions
1. **Description**: Two or more simultaneous withdrawals hit the server within the same millisecond, exploiting read-modify-write race conditions to drain more money than available.
2. **Impact**: Negative account balance and direct financial loss to the institution.
3. **Attack Scenario**: Alice has ₹1,000. She issues 10 simultaneous requests of ₹200 each. Without locking, all 10 threads read balance = ₹1,000, subtract ₹200, and write back ₹800, allowing ₹2,000 to be withdrawn.
4. **Implemented Mitigation**: Database row-level pessimistic locking (`PESSIMISTIC_WRITE` generating `SELECT ... FOR UPDATE` in PostgreSQL). Each transaction locks the account row before reading its balance. Only one thread can inspect and modify the balance at a time. Backed by PostgreSQL engine-level `CHECK (balance_paise >= 0)`.
5. **Automated Test**: `ConcurrencyTests#concurrentWithdrawalsNeverOverdraw` (10 concurrent threads; exactly 5 succeed, 5 fail, balance ends at ₹0.00).
6. **Residual Risk / Limitation**: Lock contention under extremely high burst rates. Mitigated by short transaction durations and indexed primary key lookups.

---

### THREAT-06: Ledger Imbalance
1. **Description**: A transfer debits the source account but fails to credit the destination account, destroying money.
2. **Impact**: Unbalanced books, violation of basic accounting laws, and regulatory penalties.
3. **Attack Scenario**: Database error or network drop occurs after the debit statement executes.
4. **Implemented Mitigation**: Both debit and credit movements execute within a single Spring `@Transactional` boundary. For every completed transfer, exactly two balanced `LedgerEntry` rows (`DEBIT` and `CREDIT`) referencing the same `Transaction` are created. Method `verifyGlobalDoubleEntryInvariant()` asserts that global debits strictly equal global credits.
5. **Automated Test**: `FinancialEngineTests#depositCreatesBalancedEntries` and `FinancialEngineTests#internalTransferExecutesAtomically`
6. **Residual Risk / Limitation**: Out-of-band direct database manipulation by a malicious DBA. Mitigated by restricted DB access and database replication logs.

---

### THREAT-07: Negative Balances
1. **Description**: Stale balance reads or calculation flaws cause an account to drop below zero.
2. **Impact**: Uncontrolled overdrafts and capital loss.
3. **Attack Scenario**: Rapid concurrent transactions attempt to exploit timing windows.
4. **Implemented Mitigation**: Double protection:
   - Application-level check: `if (account.getBalancePaise() < amountPaise) throw new InsufficientFundsException(...)`
   - Database-level constraint: `CONSTRAINT chk_account_balance_non_negative CHECK (balance_paise >= 0)` defined in Flyway `V1__init_schema.sql`.
5. **Automated Test**: `FinancialEngineTests#withdrawalRejectsOverdraft`
6. **Residual Risk / Limitation**: None within the application boundary.

---

### THREAT-08: SQL Injection
1. **Description**: Attacker enters malicious SQL fragments in account numbers, remarks, or query inputs.
2. **Impact**: Database compromise, unauthorized data extraction, or table truncation.
3. **Attack Scenario**: Attacker enters `' OR '1'='1` in the destination account search field.
4. **Implemented Mitigation**: Spring Data JPA uses parameterised prepared statements through Hibernate ORM. User inputs are never concatenated directly into raw SQL strings.
5. **Automated Test**: Verified across all repository finder tests.
6. **Residual Risk / Limitation**: Direct native queries if added in the future. Mitigated by codebase rule forbidding string concatenation in `@Query`.

---

### THREAT-09: Broken Authentication & Password Guessing
1. **Description**: Attacker attempts credential stuffing or brute-forcing passwords.
2. **Impact**: Account takeover.
3. **Attack Scenario**: Dictionary attack against user accounts.
4. **Implemented Mitigation**: Passwords hashed using `BCryptPasswordEncoder` with strength 12 (work factor $2^{12} = 4096$ iterations). Passwords are never stored in plaintext. Failed login attempts are logged in the append-only audit trail with IP address and timestamp.
5. **Automated Test**: `AuthenticationSecurityTests#invalidPasswordReturns401`
6. **Residual Risk / Limitation**: Weak user passwords. Mitigated by Bean Validation enforcing minimum password complexity.

---

### THREAT-10: Token Theft or Misuse
1. **Description**: Attacker intercepts or replays an authentication token.
2. **Impact**: Session hijacking.
3. **Attack Scenario**: Man-in-the-middle packet sniffing on unencrypted networks.
4. **Implemented Mitigation**: Short-lived cryptographic JWT signed with HMAC-SHA256 and verified with secret keys stored in environment variables. CORS policy restricts origin headers to authorized frontend hosts.
5. **Automated Test**: `AuthenticationSecurityTests#validLoginReturnsJwtToken`
6. **Residual Risk / Limitation**: Local storage token extraction if client device has XSS. Mitigated by Content Security Policy and avoiding inline scripts.

---

### THREAT-11: Sensitive Information Exposure
1. **Description**: Application leaks database credentials, internal file paths, or Java stack traces in API error responses.
2. **Impact**: Information disclosure assisting subsequent exploit attempts.
3. **Attack Scenario**: Attacker sends malformed JSON to trigger an unhandled runtime exception.
4. **Implemented Mitigation**: Centralized `GlobalExceptionHandler` with `@RestControllerAdvice`. All error responses are serialized into a sanitized `ApiError` format (status, error code, friendly message, path). Stack traces, passwords, and database connection strings are never returned to clients.
5. **Automated Test**: `AuthenticationSecurityTests#unauthenticatedRequestToProtectedEndpointReturns401`
6. **Residual Risk / Limitation**: Accidental developer logging of secrets. Mitigated by `.gitignore` and security reviews.

---

### THREAT-12: Audit Record Tampering
1. **Description**: A malicious actor or compromised admin attempts to alter or delete historical transaction audit logs.
2. **Impact**: Concealment of fraudulent activities and failure of regulatory audits.
3. **Attack Scenario**: Attacker calls a REST endpoint to delete an audit entry.
4. **Implemented Mitigation**: The application exposes **zero** `UPDATE` or `DELETE` endpoints on `audit_events` or `ledger_entries`. `AuditService` only performs append operations (`save`). In production PostgreSQL, database user roles can be granted `INSERT, SELECT` privileges only.
5. **Automated Test**: `AuditorController#getAuditEvents` (read-only verification).
6. **Residual Risk / Limitation**: Superuser database access. Mitigated by separate database administrative accounts and external cloud log forwarding.

---

### THREAT-13: Denial of Service (DoS) via Payload Size
1. **Description**: Attacker sends massive payloads or negative amounts to cause buffer overflows or memory exhaustion.
2. **Impact**: Service unavailability.
3. **Attack Scenario**: Attacker sends 100MB string in description or negative amounts.
4. **Implemented Mitigation**: Spring Web request size constraints; Jakarta Bean Validation `@Size(max = 255)` on descriptions, `@Min(value = 1)` on transfer amounts, and `@NotBlank` on usernames.
5. **Automated Test**: `GlobalExceptionHandler#handleValidationErrors` (returns 400 Bad Request with field details).
6. **Residual Risk / Limitation**: Distributed DoS (DDoS) at the network layer. Mitigated by cloud reverse-proxy providers (e.g. Cloudflare).

---

### THREAT-14: Database Failure During Transfer
1. **Description**: The database or server crashes midway through a multi-step transfer (after debiting Alice, before crediting Bob).
2. **Impact**: Money lost in transit; ledger inconsistency.
3. **Attack Scenario**: Power outage or database restart during transfer execution.
4. **Implemented Mitigation**: All account updates, transaction records, and ledger entries are encapsulated in a single `@Transactional` method. If any failure occurs before commit, the database engine rolls back all modifications automatically via ACID guarantees.
5. **Automated Test**: `FinancialEngineTests` transaction atomicity.
6. **Residual Risk / Limitation**: Storage media failure. Mitigated by PostgreSQL WAL (Write-Ahead Logging) and automated point-in-time recovery.

---

### THREAT-15: Deadlocks on Concurrent Transfers
1. **Description**: Alice transfers to Bob at the exact same moment Bob transfers to Alice (bidirectional transfers), creating a circular wait condition and transaction timeouts.
2. **Impact**: Failed transactions, connection exhaustion, and poor user experience.
3. **Attack Scenario**: User A -> User B and User B -> User A initiated concurrently. Thread 1 locks Account A and waits for Account B. Thread 2 locks Account B and waits for Account A.
4. **Implemented Mitigation**: Deterministic ascending lock ordering:
   ```java
   Long firstLockId = Math.min(request.getSourceAccountId(), destId);
   Long secondLockId = Math.max(request.getSourceAccountId(), destId);
   accountRepository.findByIdForUpdate(firstLockId);
   accountRepository.findByIdForUpdate(secondLockId);
   ```
   Because all threads acquire locks in the exact same numerical order of primary keys, circular lock dependencies are mathematically impossible!
5. **Automated Test**: `ConcurrencyTests#bidirectionalTransfersDoNotDeadlock` (20 bidirectional iterations executed simultaneously across 4 threads with 0 deadlocks).
6. **Residual Risk / Limitation**: Operations involving 3+ accounts. Handled by extending sorting to $N$ account IDs.
