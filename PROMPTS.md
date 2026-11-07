# LedgerLine — AI Prompt & Correction Log (PROMPTS.md)

This log records genuine interactions, recommendations, critical evaluations, and corrections made during the development of LedgerLine for the Snovasys Fresher Hiring Challenge 2026.

---

### Entry 1 — Phase 0: Environment Inspection & Build Tool Strategy
- **Date**: 2026-11-07
- **AI Tool Used**: Google Antigravity (Gemini 3.8 Flash)
- **What I asked it to do**:
  Inspect the computer environment, check installed versions of Java, Maven, Node.js, npm, Git, PostgreSQL, and Docker, confirm whether the workspace is empty, identify missing prerequisites, and propose a pinned stack.
- **What the AI generated or suggested**:
  - The AI ran environment inspection commands across Windows PowerShell.
  - The initial PowerShell scripts failed with syntax errors due to string interpolation of `$_` inside standard heredocs before switching to single-quoted script blocks.
  - The AI reported that Java (JDK 23), Node (v22.17.0), npm (10.9.2), and Git (2.54.0) were installed, but Maven (`mvn`), PostgreSQL, and Docker were missing.
  - The AI suggested installing Apache Maven globally.
- **What was incorrect, incomplete or risky**:
  1. Requiring a global system-wide Maven installation (`mvn`) introduces environment drift and configuration risks for evaluators who run the project on their own machines.
  2. The challenge specification requests Java 21 compatibility, but the installed JDK on this machine is JDK 23.
- **How I verified the issue**:
  1. Executed `mvn -version`, which returned `CommandNotFoundException`.
  2. Executed `javac -version`, confirming JDK 23 is installed at `C:\Program Files\Java\jdk-23`.
- **What I changed or rejected**:
  1. Rejected mandatory global Maven installation. Instead, we use the standard Maven Wrapper (`mvnw` / `mvnw.cmd`) pinned to Maven 3.9.9. This guarantees the evaluator can clone and run `./mvnw test` or `mvnw.cmd test` immediately without manually installing Maven.
  2. Configured the Maven compiler plugin to use `--release 21` to strictly enforce Java 21 bytecode and standard API compatibility on JDK 23.
  3. Identified that PostgreSQL 16 is available via Windows `winget` or can be paired with managed cloud PostgreSQL for production deployment.
- **Relevant Git commit**: [`c50c83e`](file:///C:/Users/venug/.gemini/antigravity/scratch/ledgerline) ("docs: initialize workspace, project status, threat model and prompts log")

---

### Entry 2 — Phase 2: Database Migration Sequence & Identity Collision
- **Date**: 2026-11-07
- **AI Tool Used**: Google Antigravity (Gemini 3.8 Flash)
- **What I asked it to do**:
  Implement Flyway migration scripts to initialize the core schema and seed the initial System Treasury Reserve Account for double-entry bookkeeping.
- **What the AI generated or suggested**:
  - The AI generated `V2__system_accounts.sql` with an explicit primary key insert: `INSERT INTO accounts (id, account_number, ...) VALUES (1, 'LL-SYS-TREASURY-01', ...)`.
  - When the application booted, `DataInitializer` attempted to create customer accounts using `accountRepository.save(new Account(...))`.
- **What was incorrect, incomplete or risky**:
  - Explicitly inserting an ID (`id = 1`) in PostgreSQL/H2 SQL migrations does not advance the underlying sequence (`accounts_id_seq`).
  - When Hibernate subsequently generated the first customer account, it called the sequence, which yielded `1`, triggering a fatal `DataIntegrityViolationException: Unique index or primary key violation on accounts(id)`.
- **How I verified the issue**:
  - Observed the stack trace in `surefire-reports/com.snovasys.ledgerline.AccountOwnershipTests.txt`:
    `Caused by: org.springframework.dao.DataIntegrityViolationException: could not execute statement [Unique index or primary key violation: "PRIMARY KEY ON public.accounts(id) ( /* key:1 */ CAST(1 AS BIGINT), 'LL-SYS-TREASURY-01'...]`
- **What I changed or rejected**:
  - Modified `V2__system_accounts.sql` to omit the hardcoded primary key `id`, allowing the database sequence to manage ID assignment naturally.
- **Relevant Git commit**: [`ec52f51`](file:///C:/Users/venug/.gemini/antigravity/scratch/ledgerline) ("feat(backend): implement double-entry ledger, concurrency locking, idempotency, RBAC, and verified tests")

---

### Entry 3 — Phase 5: Concurrent Thread Starvation & Authentication Exception Formatting
- **Date**: 2026-11-07
- **AI Tool Used**: Google Antigravity (Gemini 3.8 Flash)
- **What I asked it to do**:
  Run the automated test suite verifying multi-threaded concurrent withdrawals and unauthenticated access handling.
- **What the AI generated or suggested**:
  - The AI configured `AuditService.recordEvent` with `@Transactional(propagation = Propagation.REQUIRES_NEW)` to ensure audit logs persist even when transactions fail.
  - The AI wrote `SecurityConfig.authenticationEntryPoint` using `new ObjectMapper().writeValue(...)` to render 401 JSON errors.
- **What was incorrect, incomplete or risky**:
  1. Under the multi-threaded concurrency test (10 threads firing simultaneous withdrawals), each thread acquired a database connection for its main `@Transactional` scope and then requested a second connection for `REQUIRES_NEW` in `AuditService`. With HikariCP's default maximum pool size of 10, all 10 threads deadlocked waiting for an 11th connection from the exhausted pool (`HikariProxyConnection.rollback: Connection is closed`).
  2. `new ObjectMapper()` lacked registered Java 8 time modules, throwing `InvalidDefinitionException: Java 8 date/time type java.time.Instant not supported by default` when serializing `ApiError`.
- **How I verified the issue**:
  1. Examined `ConcurrencyTests` logs showing connection pool timeout and only 1 of 5 expected successful withdrawals completing before connection starvation.
  2. Examined `AuthenticationSecurityTests` showing 500 error instead of 401 due to Jackson `Instant` serialization failure.
- **What I changed or rejected**:
  1. Configured HikariCP connection pool `maximum-pool-size: 30`, `connection-timeout: 30000`, and `minimum-idle: 10` in both application and test profiles to provide sufficient connection headroom for concurrent nested transaction boundaries.
  2. Injected Spring Boot's pre-configured `ObjectMapper` (which includes `JavaTimeModule`) into `SecurityConfig` instead of creating an unconfigured raw instance.
  3. Re-ran `mvnw.cmd test` and verified all 17 tests passed with zero errors or failures.
- **Relevant Git commit**: [`ec52f51`](file:///C:/Users/venug/.gemini/antigravity/scratch/ledgerline) ("feat(backend): implement double-entry ledger, concurrency locking, idempotency, RBAC, and verified tests")

---
