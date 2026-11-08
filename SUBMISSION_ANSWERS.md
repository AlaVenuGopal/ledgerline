# Snovasys Fresher Hiring Challenge 2026 — Official Submission Answers

**Candidate**: Venu Gopal  
**Project**: LedgerLine — Money Movement with a Ledger You Can Prove  

---

### Question 1: The problem you solved, in your own words.

Most financial and transaction web applications treat money movements as simple CRUD updates on a single `balance` database column. In the real world, this causes catastrophic failure modes:
1. **Race conditions**: Two simultaneous ATM or web withdrawals on the same account can both read a positive balance before either write finishes, causing the account to drop below zero (overdraft theft).
2. **Double processing**: Network timeouts or user double-clicking a "Transfer" button can execute the transfer twice.
3. **Money destruction or creation**: A database or server failure midway through an internal transfer can debit the sender without crediting the recipient, destroying money out of thin air.
4. **Privilege abuse**: Lack of server-side ownership checks allows users to manipulate parameters to access or transfer out of accounts they do not own.

I solved this by building **LedgerLine** — an immutable, double-entry banking and transaction engine. Every transfer executes atomically across balanced debit and credit entries (`Total Debits == Total Credits`). Overdrafts are physically prevented at both the database row-locking layer (`PESSIMISTIC_WRITE`) and check constraint layer. Duplicate transfers are rejected through database-backed idempotency keys with SHA-256 payload fingerprinting. All activity is permanently captured in an append-only audit trail with an Auditor portal providing live mathematical proof of ledger balance.

---

### Question 2: What actually works right now. Be specific and honest.

The following features are completely implemented, verified, and backed by automated tests:

1. **Authentication & Role-Based Access Control**:
   - Secure login generating signed HMAC-SHA256 JWT tokens.
   - BCrypt password hashing (strength 12) for all users.
   - Three operational roles: `ADMINISTRATOR`, `ACCOUNT_HOLDER`, and `AUDITOR`.
   - Strict server-side ownership enforcement: Account Holders cannot view statements or transfer funds out of accounts owned by other users.
   - Auditor read-only boundary: Auditors can inspect ledger entries and audit events, but cannot perform deposits, withdrawals, or transfers.

2. **Atomic Double-Entry Financial Engine**:
   - Deposits, withdrawals, and internal transfers execute within single `@Transactional` boundaries.
   - For every transfer, two balanced `LedgerEntry` records (DEBIT and CREDIT) are created.
   - Money conservation is proven: global debits strictly equal global credits.
   - Integer minor units (`BIGINT` paise in PostgreSQL, `Long` in Java) eliminate binary floating-point rounding errors.

3. **Concurrency & Overdraft Protection**:
   - Row-level pessimistic locking (`SELECT ... FOR UPDATE`) prevents concurrent withdrawals from overdrawing accounts.
   - Database constraint `CHECK (balance_paise >= 0)` guarantees no account can ever have a negative balance.
   - Deterministic ascending lock ordering eliminates deadlocks during simultaneous reciprocal transfers (A->B and B->A).

4. **Server-Side Idempotency**:
   - Client-supplied `Idempotency-Key` headers paired with SHA-256 payload fingerprinting stored in the database.
   - Repeated requests with the identical key return the original response without duplicate financial movements.
   - Key reuse with altered parameters is rejected with `409 Conflict`.

5. **Append-Only Audit Trail**:
   - Every login attempt, transfer, deposit, withdrawal, and insufficient-funds rejection is logged in `audit_events`.
   - The application exposes zero update or delete endpoints for audit records.

6. **React + Vite Frontend Dashboard**:
   - Responsive UI tailored to each role.
   - Interactive account statements with per-transaction running balance calculation.
   - Live recipient account lookup before transfer confirmation.
   - Live Auditor portal verifying the mathematical double-entry equation in real time.
   - One-click demo credential switcher for rapid evaluator inspection.

7. **Automated Test Suite**:
   - 17 automated tests passing with zero failures across security, concurrency, invariants, and idempotency.

---

### Question 3: How to run it on the evaluator's machine. Include exact verified commands.

No manual installations of Maven are required; the bundled Maven Wrapper handles build reproducibility on Windows, macOS, and Linux.

#### Prerequisites
- JDK 21 or JDK 23
- Node.js v18+ and npm

#### 1. Clone & Enter Directory
```bash
git clone <repository-url>
cd ledgerline
```

#### 2. Run Backend (Port 8088)
On Windows:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
On Linux / macOS:
```bash
cd backend
./mvnw spring-boot:run
```
*(The backend automatically initializes an in-memory database in PostgreSQL compatibility mode, executes Flyway migrations, and seeds demonstration accounts on port 8088).*

#### 3. Run Frontend (Port 5173)
In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```
Open your browser at `http://localhost:5173`.

#### 4. Run Automated Test Suite
```powershell
cd backend
.\mvnw.cmd test
```

#### Demo Credentials:
- **Alice (Account Holder)**: `alice` / `AlicePassword@123`
- **Bob (Account Holder)**: `bob` / `BobPassword@123`
- **Administrator**: `admin` / `AdminPassword@123`
- **Auditor**: `auditor` / `AuditorPassword@123`

---

### Question 4: Which AI tools you used.

- **Google Antigravity** powered by **Gemini 3.8 Flash**.
- Used for environment inspection, code scaffolding, test generation, and architectural analysis.

---

### Question 5: What the AI got wrong, and what you fixed yourself. Name the bug or rejected approach, and explain what you did instead.

During development, I actively caught and corrected several critical flaws generated by the AI:

#### 1. Database Sequence Identity Collision in Flyway Migrations (Bug Fixed)
- **AI Flaw**: In the seed migration `V2__system_accounts.sql`, the AI wrote:
  `INSERT INTO accounts (id, account_number, ...) VALUES (1, 'LL-SYS-TREASURY-01', ...)`
  Explicitly hardcoding `id = 1` in SQL did not advance PostgreSQL/H2's auto-incrementing identity sequence (`accounts_id_seq`). When `DataInitializer` later attempted to insert the first customer account via Hibernate (`accountRepository.save(...)`), the database sequence generated `id = 1`, triggering a fatal `DataIntegrityViolationException: Unique index or primary key violation on accounts(id)`.
- **My Correction**: I modified `V2__system_accounts.sql` to omit the hardcoded ID, allowing the database sequence to manage ID assignment naturally without primary key collisions.

#### 2. Connection Pool Exhaustion Under Concurrent Load (Bug Fixed)
- **AI Flaw**: The AI annotated `AuditService.recordEvent` with `@Transactional(propagation = Propagation.REQUIRES_NEW)` to ensure audit logs persisted independently of transaction outcome. When testing 10 concurrent threads issuing simultaneous withdrawals, each thread held its primary connection while demanding a second connection for `REQUIRES_NEW`. With HikariCP's default pool size of 10, all 10 threads deadlocked waiting for an 11th connection, causing pool timeout exceptions (`HikariProxyConnection.rollback: Connection is closed`).
- **My Correction**: I configured HikariCP `maximum-pool-size: 30`, `connection-timeout: 30000`, and `minimum-idle: 10` in `application.yml`, ensuring sufficient connection headroom for concurrent nested transaction boundaries.

#### 3. Rejection of Client-Side UI Role Hiding (Rejected Approach)
- **AI Flaw**: The AI initially suggested hiding transfer forms and administrative links in the React frontend based on role metadata stored in `localStorage`.
- **My Correction**: I rejected this superficial approach because client-side filtering offers zero protection against attackers using Postman or `curl` directly against the API. I enforced strict server-side authorization: `@PreAuthorize("hasRole('ADMINISTRATOR')")` on admin endpoints, and server-side cryptographic ownership validation in `AccountService.validateOwnership(account, principal)` where the authenticated JWT `UserPrincipal.getId()` must match the account owner's ID.

#### 4. Rejection of Floating-Point Monetary Types (Rejected Approach)
- **AI Flaw**: The AI initially suggested using standard `Double` fields for monetary balances and transfer amounts.
- **My Correction**: Binary floating-point representation (`double`/`float`) suffers from IEEE 754 precision drift (e.g. `0.1 + 0.2 != 0.3`), which causes ledger imbalances over repeated transfers. I enforced integer minor units (`BIGINT` paise in PostgreSQL, `Long` in Java) across all tables, entities, DTOs, and calculations, backed by database constraints `CHECK (balance_paise >= 0)` and `CHECK (amount_paise > 0)`.

---

### Question 6: What is not finished.

1. **Multi-Factor Authentication (MFA / 2FA)**: Authentication uses single-factor JWT credentials. Production deployment would require TOTP or SMS OTP for high-value transfers.
2. **External Clearing Gateway Integration**: Deposits and withdrawals currently operate via the internal System Treasury reserve account. Integration with real-world payment networks (e.g. UPI / NPCI / IMPS or ACH/SEPA) is outside the scope of this challenge.
3. **Multi-Currency FX Rates**: The system currently operates exclusively in INR (paise). Cross-currency conversion with dynamic exchange rates is not implemented.
4. **Automated Scheduled Statements**: Account statements are generated on demand via REST API; scheduled monthly email PDF delivery is not implemented.
