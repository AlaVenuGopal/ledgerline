# LedgerLine — Money Movement with a Ledger You Can Prove

> **Snovasys Fresher Hiring Challenge 2026**  
> **Candidate & Author**: Venu Gopal  
> **Repository Status**: Private during development (per challenge submission instructions)  

---

## 1. Problem Statement

Financial applications suffer catastrophic losses when money movement is treated as naive CRUD operations:
- **Duplicate Transfers**: Double-clicking or network retries transfer money twice.
- **Concurrent Overdrafts**: Simultaneous withdrawals exploit read-modify-write race conditions to make accounts negative.
- **Unbalanced Ledgers**: A system crash midway through an internal transfer debits one account without crediting the other, creating or destroying money.
- **Unauthorized Data & Privilege Access**: Account holders accessing other users' balances or executing unauthorized actions.

**LedgerLine** solves this by establishing a mathematically verifiable, double-entry banking and transaction engine where every single money movement is accurate, authorised, traceable, idempotent, and provably balanced.

---

## 2. Core Architectural Guarantees

1. **Money is Never Created or Destroyed**: Every transfer, deposit, and withdrawal creates balanced debit and credit entries (`SUM(Debits) == SUM(Credits)`).
2. **Zero-Overdraft Concurrency Protection**: Database row-level pessimistic locking (`PESSIMISTIC_WRITE` / `SELECT FOR UPDATE`) prevents simultaneous withdrawals from overdrawing an account. Backed by engine-level `CHECK (balance_paise >= 0)`.
3. **Deterministic Deadlock Avoidance**: Multi-account transfers sort account IDs in ascending order before locking, mathematically eliminating circular wait conditions on reciprocal transfers (A->B and B->A).
4. **Genuine Server-Side Idempotency**: Client-supplied `Idempotency-Key` headers paired with SHA-256 payload fingerprinting stored in the database. Duplicate requests replay the original response without moving money twice; modified payloads are rejected with `409 Conflict`.
5. **Integer Minor Units (Paise)**: Financial balances are stored strictly as integer minor units (`BIGINT` paise in PostgreSQL, `Long` in Java), eliminating binary floating-point rounding errors.
6. **Server-Side Role-Based Access Control**: Centralized Spring Security authorization enforces three roles: `ADMINISTRATOR`, `ACCOUNT_HOLDER`, and `AUDITOR` with cryptographic ownership verification.

---

## 3. Technology Stack

### Backend
- **Language**: Java 21 (`--release 21`)
- **Framework**: Spring Boot 3.3.5
- **Security**: Spring Security + BCrypt (strength 12) + Stateless JWT (JJWT 0.12.6)
- **Data & ORM**: Spring Data JPA / Hibernate ORM
- **Database Migrations**: Flyway 10.x
- **Build Tool**: Maven Wrapper (`mvnw` / `mvnw.cmd`) pinned to Apache Maven 3.9.9
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)

### Frontend
- **Framework**: React 18 / 19
- **Tooling**: Vite 8 (Ultra-fast ES modules & bundler)
- **Routing**: React Router DOM 6
- **Icons**: Lucide React
- **Design**: Responsive Custom Fintech Design System (CSS variables, high contrast, mobile & desktop layouts)

### Database
- **Engine**: PostgreSQL 16 (Production) / In-Memory PostgreSQL Compatibility Mode (Development/Test)

---

## 4. Project Structure

```text
ledgerline/
├── .gitignore                      # Excludes secrets, .env, target/, node_modules/
├── .env.example                    # Template environment variables
├── README.md                       # Comprehensive documentation
├── THREATS.md                      # 15-point security threat model
├── PROMPTS.md                      # Verifiable AI prompt & correction log (25 marks)
├── PROJECT_STATUS.md               # Living quality gate dashboard
├── SUBMISSION_ANSWERS.md           # Draft answers for official Snovasys form
│
├── backend/                        # Spring Boot Core Financial Engine
│   ├── mvnw / mvnw.cmd             # Official Maven Wrapper
│   ├── pom.xml                     # Pinned Maven dependencies
│   ├── src/main/java/com/snovasys/ledgerline/
│   │   ├── config/                 # SecurityConfig, DataInitializer
│   │   ├── controller/             # Auth, Account, Transaction, Admin, Auditor, Health
│   │   ├── dto/                    # Request/Response DTOs & ApiError
│   │   ├── entity/                 # User, Account, Transaction, LedgerEntry, IdempotencyRecord, AuditEvent
│   │   ├── exception/              # GlobalExceptionHandler & Domain Exceptions
│   │   ├── repository/             # Spring Data JPA with PESSIMISTIC_WRITE locks
│   │   ├── security/               # JwtTokenProvider, JwtFilter, UserPrincipal
│   │   └── service/                # FinancialEngineService, AccountService, AuthService, AuditService
│   └── src/main/resources/
│       ├── application.yml         # Core configuration
│       ├── application-postgres.yml# PostgreSQL production profile
│       └── db/migration/           # Flyway V1 (schema) & V2 (treasury account)
│
└── frontend/                       # React + Vite Financial Dashboard
    ├── package.json                # React, Vite, React Router, Lucide
    ├── vite.config.js              # Vite configuration
    └── src/
        ├── components/             # Sidebar, Topbar
        ├── context/                # AuthContext (JWT session management)
        ├── pages/                  # Login, Dashboard, Accounts, Transfer, Deposit/Withdraw, Transactions, Auditor, Admin
        ├── services/               # Centralized API service layer
        └── index.css               # Responsive design system
```

---

## 5. Prerequisites & Environment

- **Operating System**: Windows 10/11, macOS, or Linux
- **Java**: JDK 21 (or JDK 23 with `--release 21`)
- **Node.js**: v18+ (verified on v22.17.0)
- **npm**: 9+ (verified on 10.9.2)
- **PostgreSQL**: Optional for local testing (H2 in PostgreSQL mode included by default); required for production deployment.

---

## 6. Setup & Run Instructions (Windows Verified)

### A. Run Backend
From the project root:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
The backend starts on `http://localhost:8080`. Flyway automatically executes migrations and `DataInitializer` seeds demonstration accounts.

### B. Run Frontend
In a separate terminal:
```powershell
cd frontend
npm install
npm run dev
```
The frontend starts on `http://localhost:5173`.

---

## 7. Demo Accounts & Evaluation Credentials

The application automatically seeds four distinct accounts with secure BCrypt passwords:

| Role | Username | Password | Purpose |
|---|---|---|---|
| **Account Holder** | `alice` | `AlicePassword@123` | Testing transfers, statements, deposits, and overdraft limits (Initial: ₹50,000.00) |
| **Account Holder** | `bob` | `BobPassword@123` | Receiving transfers, withdrawals, and independent account statements (Initial: ₹25,000.00) |
| **Administrator** | `admin` | `AdminPassword@123` | User provisioning, opening customer accounts, system administration |
| **Auditor** | `auditor` | `AuditorPassword@123` | Read-only compliance portal, live Double-Entry Ledger Proof, append-only audit trail |

*Quick login buttons are available on the login page to switch between roles with a single click.*

---

## 8. Automated Testing & Verification

LedgerLine includes an automated test suite verifying security, invariants, idempotency, and concurrency:

```powershell
cd backend
.\mvnw.cmd test
```

### Verified Test Suite (17 Tests, 0 Failures):
1. **`AuthenticationSecurityTests`**:
   - `validLoginReturnsJwtToken`: Verifies valid login returns cryptographic JWT.
   - `invalidPasswordReturns401`: Verifies incorrect password returns 401 with sanitized error.
   - `unauthenticatedRequestToProtectedEndpointReturns401`: Verifies protected endpoints reject unauthenticated calls.
2. **`RoleAuthorizationSecurityTests`**:
   - `accountHolderDeniedAdminEndpoint`: Proves Account Holder cannot call `/api/admin/*` (403 Forbidden).
   - `auditorDeniedTransferExecution`: Proves Auditor cannot execute money movements (403 Forbidden).
   - `adminCanAccessAdminUsers`: Proves Administrator can access administrative users.
   - `auditorCanAccessAuditLogs`: Proves Auditor can access read-only audit trail.
3. **`AccountOwnershipTests`**:
   - `aliceCannotWithdrawFromBobsAccount`: Proves Alice cannot withdraw from Bob's account (403 Forbidden).
   - `aliceCannotTransferOutOfBobsAccount`: Proves Alice cannot transfer out of Bob's account (403 Forbidden).
   - `aliceCannotViewBobsStatement`: Proves Alice cannot view Bob's account statement (403 Forbidden).
4. **`FinancialEngineTests`**:
   - `depositCreatesBalancedEntries`: Verifies atomic balance update and balancing debit/credit entries.
   - `withdrawalRejectsOverdraft`: Verifies insufficient funds rejection leaves balance intact.
   - `internalTransferExecutesAtomically`: Verifies money conservation across internal transfers.
5. **`IdempotencyTests`**:
   - `duplicateIdempotencyKeyReplaysResponseWithoutRetransfer`: Verifies duplicate requests do not move money twice.
   - `keyReusedWithDifferentPayloadThrowsConflict`: Verifies key reused with altered payload is rejected (409 Conflict).
6. **`ConcurrencyTests`**:
   - `concurrentWithdrawalsNeverOverdraw`: 10 simultaneous threads fire withdrawals on an account with ₹1,000. Exactly 5 succeed, 5 are rejected, ending balance is strictly ₹0.00.
   - `bidirectionalTransfersDoNotDeadlock`: 20 concurrent bidirectional transfers (A->B and B->A) execute across 4 threads without deadlocks due to deterministic lock ordering.

---

## 9. API Reference

### Authentication
- `POST /api/auth/login` — Authenticate credentials, returns JWT token.

### Accounts
- `GET /api/accounts` — List accounts owned by caller.
- `GET /api/accounts/{id}` — Get account details (enforces ownership).
- `GET /api/accounts/{id}/statement` — Get statement with running balances.
- `GET /api/accounts/lookup/{accountNumber}` — Verify destination account holder.

### Financial Transactions
- `POST /api/transactions/deposit` — Deposit funds (requires `Idempotency-Key`).
- `POST /api/transactions/withdraw` — Withdraw funds (requires `Idempotency-Key`, overdraft protected).
- `POST /api/transactions/transfer` — Transfer money (requires `Idempotency-Key`, atomic double-entry).
- `GET /api/transactions` — Query transaction history with ledger entries.

### Administration & Audit
- `GET /api/admin/users` — List system users (`ADMINISTRATOR` only).
- `POST /api/admin/users` — Create user (`ADMINISTRATOR` only).
- `POST /api/admin/accounts` — Provision account (`ADMINISTRATOR` only).
- `GET /api/auditor/audit-events` — View append-only audit trail (`AUDITOR` / `ADMINISTRATOR`).
- `GET /api/auditor/ledger-integrity` — Live mathematical proof that `SUM(Debits) == SUM(Credits)`.

---

## 10. Security Notes

- **No Secrets in Code**: Environment credentials use `.env` (ignored by Git).
- **Sanitized Errors**: No stack traces or database connection details exposed to clients.
- **Append-Only Auditing**: Audit logs cannot be updated or deleted through the API.
- **CSRF & CORS**: Stateless JWT authentication with strict origin filtering.
