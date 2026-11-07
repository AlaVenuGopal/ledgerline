# LedgerLine — Threat Model & Security Architecture (THREATS.md)

This document provides a systematic threat analysis for **LedgerLine**, in alignment with financial industry security standards and the Snovasys Fresher Hiring Challenge 2026 requirements.

For every threat, we define:
1. Threat Description
2. Impact
3. Attack Scenario
4. Implemented Mitigation
5. Automated Test That Validates the Mitigation
6. Residual Risk or Limitation

---

## Threat Matrix Overview

| ID | Threat Category | Description | Primary Mitigation | Verification Test |
|---|---|---|---|---|
| **THREAT-01** | Unauthorised Account Access | Attacker attempts to view or operate accounts without valid authentication | Spring Security stateless session / JWT filter with 401 Unauthorized | `AuthenticationSecurityTests#unauthenticatedRequestsRejected` |
| **THREAT-02** | Horizontal Privilege Escalation | Account Holder A attempts to view, withdraw from, or transfer out of Account Holder B's account | Server-side ownership verification (`account.getOwnerId().equals(principal.getId())`) | `AccountAuthorizationTests#cannotAccessOtherUsersAccount` |
| **THREAT-03** | Vertical Privilege Escalation | Account Holder attempts to invoke Administrator or Auditor administrative endpoints | Role-based access control (`@PreAuthorize("hasRole('ADMIN')")`) | `RoleSecurityTests#accountHolderDeniedAdminEndpoints` |
| **THREAT-04** | Duplicate Transaction Requests | Network retry or double-click causes the same transfer to execute twice | Database-enforced Idempotency Key with unique constraint and payload fingerprinting | `IdempotencyTests#duplicateKeyReturnsSameResultWithoutRetransfer` |
| **THREAT-05** | Concurrent Withdrawal Race Conditions | Attacker issues simultaneous withdrawal requests to drain more than available balance | Database row-level pessimistic locking (`PESSIMISTIC_WRITE` / `SELECT FOR UPDATE`) + DB check constraint | `ConcurrencyTests#concurrentWithdrawalsCannotOverdraw` |
| **THREAT-06** | Ledger Imbalance | System records transfer debit without matching credit, or unequal amounts | Atomic database transaction (`@Transactional`) + Double-Entry invariant checks (sum debits == sum credits) | `LedgerInvariantTests#moneyConservationInvariant` |
| **THREAT-07** | Negative Balances | Account balance drops below zero due to stale read or missing constraint | PostgreSQL `CHECK (balance_paise >= 0)` constraint + lock-before-check | `AccountSafetyTests#negativeBalanceRejectedAtDatabaseLevel` |
| **THREAT-08** | SQL Injection | Attacker supplies malicious SQL fragments in account numbers, remarks, or query filters | Spring Data JPA parameterised queries; Hibernate ORM; strictly typed inputs | `SecurityInjectionTests#maliciousPayloadsTreatedAsLiterals` |
| **THREAT-09** | Broken Authentication / Brute Force | Attacker tries common or guessed passwords | BCrypt password hashing (strength 12); rate limiting / lockout policy | `AuthenticationTests#invalidPasswordRejected` |
| **THREAT-10** | Token Theft or Misuse | Eavesdropping or XSS extracts auth credentials | HTTPS enforcement; short-lived tokens; secure HTTP headers | `SecurityHeadersTests#securityHeadersConfigured` |
| **THREAT-11** | Sensitive Information Exposure | Stack traces or database credentials leaked in API error responses or logs | Centralized `@RestControllerAdvice` returning sanitised `ApiError`; `.gitignore` for secrets | `ApiSecurityTests#sanitizedErrorResponses` |
| **THREAT-12** | Audit Record Tampering | Rogue actor attempts to modify or delete historical audit log entries | Append-only database table; no UPDATE or DELETE exposed in application layer; DB trigger guard | `AuditTrailTests#auditLogsImmutable` |
| **THREAT-13** | Denial of Service (DoS) via Payload Size | Large payload or high-frequency automated requests saturate server | Request size limits; connection timeouts; input validation with Bean Validation | `ValidationTests#oversizedPayloadRejected` |
| **THREAT-14** | Database Failure During Transfer | Server or DB crashes midway through a multi-step financial movement | ACID transaction rollback ensuring zero partial writes | `TransactionRollbackTests#failureRollsBackEntireTransfer` |
| **THREAT-15** | Deadlocks on Concurrent Transfers | Two users transfer between each other simultaneously (A->B and B->A) causing deadlocks | Deterministic lock ordering (e.g., lock accounts sorted by account ID) | `ConcurrencyTests#biDirectionalConcurrentTransfersDoNotDeadlock` |

---

*Detailed threat breakdowns and exact test mappings will be continuously expanded and cross-referenced as each subsystem is implemented.*
