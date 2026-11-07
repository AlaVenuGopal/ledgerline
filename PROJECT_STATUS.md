# LedgerLine — Project Status & Execution Log

**Challenge**: Snovasys Fresher Hiring Challenge 2026  
**Candidate & Project Owner**: Venu Gopal  
**Architecture**: React + Vite (Frontend) | Java 21 / Spring Boot 3.3.x (Backend) | PostgreSQL + Flyway (Database)  
**Last Updated**: 2026-11-07  

---

## 1. Quality Gate Status

- [ ] Backend starts successfully.
- [ ] Frontend starts successfully.
- [ ] PostgreSQL connection works.
- [ ] Database migrations work (Flyway).
- [ ] Authentication works (BCrypt + JWT/Session).
- [ ] All three roles work (ADMINISTRATOR, ACCOUNT_HOLDER, AUDITOR).
- [ ] Account ownership is enforced on the server.
- [ ] Deposits work with minor units (paise).
- [ ] Withdrawals work with overdraft prevention.
- [ ] Transfers work atomically.
- [ ] Double-entry ledger balances (Debits = Credits).
- [ ] Money-conservation invariant is tested.
- [ ] Negative balances are prevented at DB constraint & lock level.
- [ ] Idempotency is tested (sequential & concurrent duplicates).
- [ ] Concurrent withdrawals are tested against PostgreSQL (pessimistic locking).
- [ ] Concurrent transfers are tested.
- [ ] Auditor is read-only.
- [ ] Audit trail works (append-only events).
- [ ] Frontend APIs work and reflect real backend data.
- [ ] Production builds pass (Vite build + Maven package).
- [ ] README.md is accurate with verified commands.
- [ ] THREATS.md is accurate with mitigations and test mappings.
- [ ] PROMPTS.md has genuine dated entries.
- [ ] Deployment works (Live frontend, backend, PostgreSQL).
- [ ] Live URL opens in incognito mode without errors.
- [ ] Demo credentials work.
- [ ] Six submission answers are prepared and verified.

---

## 2. Execution Phases & Progress

| Phase | Description | Status | Verification / Evidence |
|---|---|---|---|
| **Phase 0** | Environment Inspection & Project Setup | **IN PROGRESS** | Node 22, npm 10, Git 2.54, JDK 23 verified. Missing: PostgreSQL service, global Maven. |
| **Phase 1** | Project Foundation (Scaffolding & Wrapper) | PENDING | Scaffold backend (Spring Boot 3.3.5 / Java 21) & frontend (Vite React). |
| **Phase 2** | Database & Architecture (Schema & Migrations) | PENDING | Flyway migrations, double-entry ledger schema, minor units (paise). |
| **Phase 3** | Authentication & Authorisation (RBAC) | PENDING | SecurityConfig, 3 roles, account ownership validation. |
| **Phase 4** | Financial Engine (Deposit, Withdraw, Transfer) | PENDING | Atomic balance transactions, debit/credit matching. |
| **Phase 5** | Financial Safety (Idempotency, Concurrency, Audit) | PENDING | DB idempotency keys, pessimistic write locks, append-only audit trail. |
| **Phase 6** | Frontend Dashboard & Forms | PENDING | Role-based views, accessible forms, live API integration. |
| **Phase 7** | Testing & Verification | PENDING | Concurrency tests, unit tests, integration tests. |
| **Phase 8** | Documentation & Deployment | PENDING | README.md, THREATS.md, cloud deployment. |
| **Phase 9** | Final Submission Materials | PENDING | 6 answers draft, demo video script, final quality gate. |

---

## 3. Environment Summary

- **OS**: Windows 11 64-bit (Lenovo IdeaPad Slim 3)
- **Drive C: Free Space**: 40.3 GB (Used: 214 GB)
- **Git**: 2.54.0.windows.1 (Installed)
- **Node.js**: v22.17.0 (Installed)
- **npm**: 10.9.2 (Installed)
- **Java / JDK**: Oracle JDK 23 (`C:\Program Files\Java\jdk-23`), `JAVA_HOME` set
- **Maven**: Maven wrapper (`mvnw.cmd`) will be pinned to Maven 3.9.9 for guaranteed reproducible builds without requiring global path dependencies.
- **PostgreSQL**: Not currently running locally. Option A: install PostgreSQL 16 via winget (`winget install PostgreSQL.PostgreSQL.16`); Option B: free managed cloud PostgreSQL (e.g., Neon / Supabase / Aiven) or Docker.
- **Docker**: Not installed.

---

## 4. Current Blockers & Critical Decisions

1. **PostgreSQL Availability**:
   - To execute integration & concurrency tests against real PostgreSQL locking semantics (pessimistic locks `SELECT FOR UPDATE`), we need a live PostgreSQL instance.
   - Recommended action: Install PostgreSQL 16 on Windows using `winget install PostgreSQL.PostgreSQL.16 --silent` OR configure a cloud test/dev PostgreSQL instance.

---

## 5. Recent Git Commits

- (Repo initialized)
