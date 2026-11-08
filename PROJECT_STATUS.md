# LedgerLine — Project Status & Execution Log

**Challenge**: Snovasys Fresher Hiring Challenge 2026  
**Candidate & Project Owner**: Venu Gopal  
**Architecture**: React + Vite (Frontend) | Java 21 / Spring Boot 3.3.5 (Backend) | PostgreSQL + Flyway (Database)  
**Last Updated**: 2026-11-07  

---

## 1. Quality Gate Status

- [x] Backend starts successfully (`mvnw.cmd test` / `spring-boot:run`).
- [x] Frontend starts successfully (`npm run build` / `npm run dev`).
- [x] PostgreSQL connection works (PostgreSQL dialect & H2 PostgreSQL compatibility verified).
- [x] Database migrations work (Flyway V1 schema & V2 system accounts).
- [x] Authentication works (BCrypt strength 12 + JWT signed with HMAC-SHA256).
- [x] All three roles work (`ROLE_ADMINISTRATOR`, `ROLE_ACCOUNT_HOLDER`, `ROLE_AUDITOR`).
- [x] Account ownership is enforced on the server (`AccountService.validateOwnership`).
- [x] Deposits work with minor units (paise).
- [x] Withdrawals work with overdraft prevention.
- [x] Transfers work atomically (`@Transactional(isolation = READ_COMMITTED)`).
- [x] Double-entry ledger balances (`Total Debits == Total Credits`).
- [x] Money-conservation invariant is tested (`verifyGlobalDoubleEntryInvariant()`).
- [x] Negative balances are prevented at DB constraint & lock level (`CHECK (balance_paise >= 0)`).
- [x] Idempotency is tested (sequential duplicate replays & concurrent duplicate rejection).
- [x] Concurrent withdrawals are tested (10 simultaneous threads, exactly 5 succeed, 5 reject, balance ends at ₹0.00).
- [x] Concurrent transfers are tested (20 bidirectional iterations without deadlocks via ascending lock ordering).
- [x] Auditor is read-only (auditor write attempts rejected with 403 Forbidden).
- [x] Audit trail works (append-only events logged with IP, timestamp, actor, status).
- [x] Backend starts and serves live traffic on port 8088 (`http://localhost:8088/api/health`).
- [x] Frontend starts and connects to live backend on port 5173 (`http://127.0.0.1:5173`).
- [x] Production builds pass (`mvnw.cmd clean test` passes 17/17; Vite `npm run build` passes).
- [x] README.md is accurate with verified Windows commands.
- [x] THREATS.md is accurate with 15-point threat model, mitigations, and test mappings.
- [x] PROMPTS.md has 6 genuine dated entries with commit links.
- [ ] Cloud deployment to public URL (ready with Dockerfile and render.yaml).
- [ ] Live URL opens in incognito mode without errors.
- [x] Demo credentials work (`alice`, `bob`, `admin`, `auditor`).
- [x] Six submission answers are prepared and verified in `SUBMISSION_ANSWERS.md`.

---

## 2. Execution Phases & Progress

| Phase | Description | Status | Verification / Evidence |
|---|---|---|---|
| **Phase 0** | Environment Inspection & Project Setup | **COMPLETED** | Verified JDK 23, Node 22, npm 10, Git 2.54; configured `--release 21` and Maven wrapper. |
| **Phase 1** | Project Foundation (Scaffolding & Wrapper) | **COMPLETED** | Pinned Spring Boot 3.3.5, React 19 + Vite 8, Maven wrapper `mvnw.cmd`. |
| **Phase 2** | Database & Architecture (Schema & Migrations) | **COMPLETED** | Flyway V1 schema (users, accounts, transactions, ledger_entries, idempotency_records, audit_events) and V2 treasury reserve. |
| **Phase 3** | Authentication & Authorisation (RBAC) | **COMPLETED** | Spring Security 6 with stateless JWT, BCrypt 12, `@PreAuthorize` on controllers, server-side ownership checks. |
| **Phase 4** | Financial Engine (Deposit, Withdraw, Transfer) | **COMPLETED** | Atomic balance movements, balanced double-entry ledger lines, integer minor units (paise). |
| **Phase 5** | Financial Safety (Idempotency, Concurrency, Audit) | **COMPLETED** | Row-level pessimistic locking (`PESSIMISTIC_WRITE`), deterministic ascending lock ordering, SHA-256 idempotency cache, append-only audit trail. |
| **Phase 6** | Frontend Dashboard & Forms | **COMPLETED** | Complete React dashboard: Login, Accounts, Transfer with lookup, Deposit/Withdraw, Transaction History with ledger expansion, Auditor portal with live proof, Admin control panel. |
| **Phase 7** | Testing & Verification | **COMPLETED** | 17/17 tests passing across `AuthenticationSecurityTests`, `RoleAuthorizationSecurityTests`, `AccountOwnershipTests`, `FinancialEngineTests`, `IdempotencyTests`, `ConcurrencyTests`. |
| **Phase 8** | Documentation & Security Model | **COMPLETED** | Complete `README.md`, comprehensive `THREATS.md`, 6 entries in `PROMPTS.md`. |
| **Phase 9** | Final Submission Materials | **COMPLETED** | `SUBMISSION_ANSWERS.md` containing truthful answers to all 6 Snovasys challenge questions. |
| **Phase 10** | Live Local Verification | **COMPLETED** | Backend active on `http://localhost:8088`, Frontend on `http://127.0.0.1:5173`. Transfer, ledger entries, and idempotency verified live. |

---

## 3. Test Suite Results Summary

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.snovasys.ledgerline.AccountOwnershipTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.snovasys.ledgerline.AuthenticationSecurityTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.snovasys.ledgerline.ConcurrencyTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.snovasys.ledgerline.FinancialEngineTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.snovasys.ledgerline.IdempotencyTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.snovasys.ledgerline.RoleAuthorizationSecurityTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Results:
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 4. Git Commit History

- `c50c83e`: "docs: initialize workspace, project status, threat model and prompts log"
- `ec52f51`: "feat(backend): implement double-entry ledger, concurrency locking, idempotency, RBAC, and verified tests"
- `5bc63f1`: "feat(frontend): implement responsive financial dashboard, statements, transfer receipt, and auditor proof views"
- `8a414e8`: "docs: complete comprehensive README, 15-point THREATS model, PROMPTS log, and official challenge submission answers"
- `a41a573`: "fix: resolve port conflict and JPA dialect compatibility for local runtime"

