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

### Entry 4 — Phase 3: Server-Side Authorisation vs Frontend UI Hiding
- **Date**: 2026-11-07
- **AI Tool Used**: Google Antigravity (Gemini 3.8 Flash)
- **What I asked it to do**:
  Implement authorization and role boundaries to ensure Account Holders cannot perform administrative tasks or access other users' accounts, and Auditors cannot initiate transactions.
- **What the AI generated or suggested**:
  - The AI initially suggested hiding the "Transfer" button and "Admin" navigation links in the React frontend based on the user's role stored in `localStorage`.
- **What was incorrect, incomplete or risky**:
  - Client-side hiding provides zero security in a financial banking engine.
  - Anyone with browser developer tools or a tool like Postman / `curl` can forge HTTP requests directly to `/api/transactions/transfer` or `/api/admin/users`, completely bypassing React UI checks.
  - Relying on client-supplied user IDs or role claims in request bodies introduces Insecure Direct Object Reference (IDOR) and horizontal privilege escalation.
- **How I verified the issue**:
  - Sent forged curl requests with an Account Holder's JWT token to `/api/admin/users` and `/api/transactions/transfer` with another user's account ID.
- **What I changed or rejected**:
  - Rejected client-only role filtering as an authorization mechanism.
  - Enforced strict backend role-based access control with `@PreAuthorize("hasRole('ADMINISTRATOR')")` and `@PreAuthorize("hasAnyRole('ACCOUNT_HOLDER', 'ADMINISTRATOR')")` on all Spring controllers.
  - Implemented server-side ownership validation in `AccountService.validateOwnership(account, principal)`: the authenticated `UserPrincipal.getId()` extracted directly from the verified cryptographic JWT must match the account owner's ID, or a 403 Forbidden is thrown.
  - Validated with automated tests `RoleAuthorizationSecurityTests` and `AccountOwnershipTests`.
- **Relevant Git commit**: [`ec52f51`](file:///C:/Users/venug/.gemini/antigravity/scratch/ledgerline) ("feat(backend): implement double-entry ledger, concurrency locking, idempotency, RBAC, and verified tests")

---

### Entry 5 — Phase 4: Financial Minor Units (Paise) vs Floating-Point Precision
- **Date**: 2026-11-07
- **AI Tool Used**: Google Antigravity (Gemini 3.8 Flash)
- **What I asked it to do**:
  Design the currency handling and transaction amount data types across the frontend forms, DTOs, and database schema.
- **What the AI generated or suggested**:
  - The AI proposed using `double` or standard `Double` fields for monetary balances and transaction amounts in rupees (e.g., `Double amount = 150.50`).
- **What was incorrect, incomplete or risky**:
  - Floating-point representations (`double` / `float`) adhere to IEEE 754 standards, which cannot accurately represent decimal fractions in binary (e.g., `0.1 + 0.2 = 0.30000000000000004`).
  - In a financial ledger, rounding discrepancies accumulate over repeated transfers, creating ledger imbalance where total debits no longer equal total credits.
- **How I verified the issue**:
  - Tested running floating-point additions across 1,000 transfer iterations in a scratch script, observing cumulative cent/paisa rounding errors.
- **What I changed or rejected**:
  - Rejected all floating-point data types for financial balances.
  - Adopted strictly integer minor units (`BIGINT` paise in PostgreSQL, `Long` in Java). ₹1.00 is stored as 100 paise.
  - Configured PostgreSQL database constraints `CONSTRAINT chk_account_balance_non_negative CHECK (balance_paise >= 0)` and `CHECK (amount_paise > 0)`.
  - Frontend converts user rupee input to paise (`Math.round(rupees * 100)`) before transmission, and converts paise to rupees purely for visual formatting.
  - Validated with `FinancialEngineTests` confirming exact zero-error balance conservation.
- **Relevant Git commit**: [`ec52f51`](file:///C:/Users/venug/.gemini/antigravity/scratch/ledgerline) ("feat(backend): implement double-entry ledger, concurrency locking, idempotency, RBAC, and verified tests")

---
