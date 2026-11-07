package com.snovasys.ledgerline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snovasys.ledgerline.dto.DepositRequest;
import com.snovasys.ledgerline.dto.LedgerEntryDto;
import com.snovasys.ledgerline.dto.TransactionResponse;
import com.snovasys.ledgerline.dto.TransferRequest;
import com.snovasys.ledgerline.dto.WithdrawRequest;
import com.snovasys.ledgerline.entity.Account;
import com.snovasys.ledgerline.entity.IdempotencyRecord;
import com.snovasys.ledgerline.entity.LedgerEntry;
import com.snovasys.ledgerline.entity.Transaction;
import com.snovasys.ledgerline.exception.*;
import com.snovasys.ledgerline.repository.AccountRepository;
import com.snovasys.ledgerline.repository.IdempotencyRecordRepository;
import com.snovasys.ledgerline.repository.LedgerEntryRepository;
import com.snovasys.ledgerline.repository.TransactionRepository;
import com.snovasys.ledgerline.repository.UserRepository;
import com.snovasys.ledgerline.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialEngineService {

    private static final Logger log = LoggerFactory.getLogger(FinancialEngineService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AccountService accountService;
    private final ObjectMapper objectMapper;

    public FinancialEngineService(AccountRepository accountRepository,
                                  TransactionRepository transactionRepository,
                                  LedgerEntryRepository ledgerEntryRepository,
                                  IdempotencyRecordRepository idempotencyRecordRepository,
                                  UserRepository userRepository,
                                  AuditService auditService,
                                  AccountService accountService,
                                  ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.accountService = accountService;
        this.objectMapper = objectMapper;
    }

    // ==========================================
    // 1. DEPOSIT (Treasury DEBIT -> Account CREDIT)
    // ==========================================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse deposit(DepositRequest request, UserPrincipal principal, String idempotencyKeyHeader) {
        String key = resolveIdempotencyKey(request.getIdempotencyKey(), idempotencyKeyHeader);
        String payloadHash = calculateSha256("DEPOSIT:" + request.getAccountId() + ":" + request.getAmountPaise());

        TransactionResponse cached = checkIdempotency(key, payloadHash);
        if (cached != null) {
            log.info("Idempotent replay for deposit with key {}", key);
            return cached;
        }

        if (request.getAmountPaise() == null || request.getAmountPaise() <= 0) {
            throw new InvalidAmountException("Deposit amount must be strictly greater than 0 paise");
        }

        // Lock accounts deterministically
        Account treasury = accountRepository.findSystemTreasuryAccountForUpdate()
                .orElseThrow(() -> new LedgerlineException("System treasury account not found"));

        Account targetAccount = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + request.getAccountId()));

        accountService.validateOwnership(targetAccount, principal);

        // Update balances
        treasury.setBalancePaise(treasury.getBalancePaise() - request.getAmountPaise());
        targetAccount.setBalancePaise(targetAccount.getBalancePaise() + request.getAmountPaise());
        treasury.setUpdatedAt(Instant.now());
        targetAccount.setUpdatedAt(Instant.now());

        accountRepository.save(treasury);
        accountRepository.save(targetAccount);

        // Create Transaction
        Transaction txn = new Transaction();
        txn.setTransactionRef("DEP-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        txn.setIdempotencyKey(key);
        txn.setType("DEPOSIT");
        txn.setStatus("SUCCESS");
        txn.setAmountPaise(request.getAmountPaise());
        txn.setCurrency("INR");
        txn.setSourceAccount(treasury);
        txn.setDestinationAccount(targetAccount);
        txn.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription() : "Account Cash Deposit");
        txn.setInitiatedByUser(userRepository.getReferenceById(principal.getId()));
        Transaction savedTxn = transactionRepository.save(txn);

        // Create Double-Entry Ledger Entries
        LedgerEntry debitEntry = new LedgerEntry(savedTxn, treasury, "DEBIT", request.getAmountPaise(), treasury.getBalancePaise());
        LedgerEntry creditEntry = new LedgerEntry(savedTxn, targetAccount, "CREDIT", request.getAmountPaise(), targetAccount.getBalancePaise());

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        TransactionResponse response = new TransactionResponse(savedTxn);
        response.setLedgerEntries(List.of(new LedgerEntryDto(debitEntry), new LedgerEntryDto(creditEntry)));

        // Record Idempotency & Audit
        saveIdempotency(key, payloadHash, principal.getId(), response);
        auditService.recordEvent(
                "DEPOSIT_SUCCESS",
                principal.getId(),
                principal.getUsername(),
                targetAccount.getId(),
                savedTxn.getId(),
                "SUCCESS",
                "Deposited " + request.getAmountPaise() + " paise into account " + targetAccount.getAccountNumber(),
                null
        );

        return response;
    }

    // ==========================================
    // 2. WITHDRAWAL (Account DEBIT -> Treasury CREDIT)
    // ==========================================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse withdraw(WithdrawRequest request, UserPrincipal principal, String idempotencyKeyHeader) {
        String key = resolveIdempotencyKey(request.getIdempotencyKey(), idempotencyKeyHeader);
        String payloadHash = calculateSha256("WITHDRAW:" + request.getAccountId() + ":" + request.getAmountPaise());

        TransactionResponse cached = checkIdempotency(key, payloadHash);
        if (cached != null) {
            log.info("Idempotent replay for withdrawal with key {}", key);
            return cached;
        }

        if (request.getAmountPaise() == null || request.getAmountPaise() <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be strictly greater than 0 paise");
        }

        Account sourceAccount = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + request.getAccountId()));

        accountService.validateOwnership(sourceAccount, principal);

        Account treasury = accountRepository.findSystemTreasuryAccountForUpdate()
                .orElseThrow(() -> new LedgerlineException("System treasury account not found"));

        // Overdraft Protection
        if (sourceAccount.getBalancePaise() < request.getAmountPaise()) {
            recordRejectedTransaction(
                    "WITHDRAWAL",
                    sourceAccount,
                    treasury,
                    request.getAmountPaise(),
                    "Insufficient funds. Available: " + sourceAccount.getBalancePaise() + " paise, Required: " + request.getAmountPaise() + " paise",
                    principal,
                    key
            );
            auditService.recordEvent(
                    "WITHDRAWAL_REJECTED",
                    principal.getId(),
                    principal.getUsername(),
                    sourceAccount.getId(),
                    null,
                    "FAILURE",
                    "Withdrawal rejected: Insufficient funds in account " + sourceAccount.getAccountNumber(),
                    null
            );
            throw new InsufficientFundsException("Insufficient funds. Available: " + sourceAccount.getBalancePaise() + " paise, Required: " + request.getAmountPaise() + " paise");
        }

        // Apply balance movements
        sourceAccount.setBalancePaise(sourceAccount.getBalancePaise() - request.getAmountPaise());
        treasury.setBalancePaise(treasury.getBalancePaise() + request.getAmountPaise());
        sourceAccount.setUpdatedAt(Instant.now());
        treasury.setUpdatedAt(Instant.now());

        accountRepository.save(sourceAccount);
        accountRepository.save(treasury);

        Transaction txn = new Transaction();
        txn.setTransactionRef("WTH-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        txn.setIdempotencyKey(key);
        txn.setType("WITHDRAWAL");
        txn.setStatus("SUCCESS");
        txn.setAmountPaise(request.getAmountPaise());
        txn.setCurrency("INR");
        txn.setSourceAccount(sourceAccount);
        txn.setDestinationAccount(treasury);
        txn.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription() : "Account Cash Withdrawal");
        txn.setInitiatedByUser(userRepository.getReferenceById(principal.getId()));
        Transaction savedTxn = transactionRepository.save(txn);

        LedgerEntry debitEntry = new LedgerEntry(savedTxn, sourceAccount, "DEBIT", request.getAmountPaise(), sourceAccount.getBalancePaise());
        LedgerEntry creditEntry = new LedgerEntry(savedTxn, treasury, "CREDIT", request.getAmountPaise(), treasury.getBalancePaise());

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        TransactionResponse response = new TransactionResponse(savedTxn);
        response.setLedgerEntries(List.of(new LedgerEntryDto(debitEntry), new LedgerEntryDto(creditEntry)));

        saveIdempotency(key, payloadHash, principal.getId(), response);
        auditService.recordEvent(
                "WITHDRAWAL_SUCCESS",
                principal.getId(),
                principal.getUsername(),
                sourceAccount.getId(),
                savedTxn.getId(),
                "SUCCESS",
                "Withdrew " + request.getAmountPaise() + " paise from account " + sourceAccount.getAccountNumber(),
                null
        );

        return response;
    }

    // ==========================================
    // 3. INTERNAL TRANSFER (Source DEBIT -> Destination CREDIT)
    // ==========================================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse transfer(TransferRequest request, UserPrincipal principal, String idempotencyKeyHeader) {
        String key = resolveIdempotencyKey(request.getIdempotencyKey(), idempotencyKeyHeader);
        String destIdentifier = request.getDestinationAccountId() != null ? String.valueOf(request.getDestinationAccountId()) : request.getDestinationAccountNumber();
        String payloadHash = calculateSha256("TRANSFER:" + request.getSourceAccountId() + ":" + destIdentifier + ":" + request.getAmountPaise());

        TransactionResponse cached = checkIdempotency(key, payloadHash);
        if (cached != null) {
            log.info("Idempotent replay for transfer with key {}", key);
            return cached;
        }

        if (request.getAmountPaise() == null || request.getAmountPaise() <= 0) {
            throw new InvalidAmountException("Transfer amount must be strictly greater than 0 paise");
        }

        // Resolve destination account ID
        Long destId = request.getDestinationAccountId();
        if (destId == null) {
            if (!StringUtils.hasText(request.getDestinationAccountNumber())) {
                throw new LedgerlineException("Destination account ID or account number must be provided");
            }
            Account destLookup = accountRepository.findByAccountNumber(request.getDestinationAccountNumber().trim())
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found with number: " + request.getDestinationAccountNumber()));
            destId = destLookup.getId();
        }

        if (request.getSourceAccountId().equals(destId)) {
            throw new LedgerlineException("Self-transfers to the same account are not permitted");
        }

        // Deterministic Lock Ordering: eliminate deadlocks by locking accounts in ascending order of primary key
        Long firstLockId = Math.min(request.getSourceAccountId(), destId);
        Long secondLockId = Math.max(request.getSourceAccountId(), destId);

        Account firstLocked = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + firstLockId));
        Account secondLocked = accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + secondLockId));

        Account sourceAccount = request.getSourceAccountId().equals(firstLockId) ? firstLocked : secondLocked;
        Account destinationAccount = destId.equals(firstLockId) ? firstLocked : secondLocked;

        // Authorisation: verify caller owns source account
        accountService.validateOwnership(sourceAccount, principal);

        if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())) {
            throw new LedgerlineException("Source account is not active");
        }
        if (!"ACTIVE".equalsIgnoreCase(destinationAccount.getStatus())) {
            throw new LedgerlineException("Destination account is not active");
        }

        // Overdraft Protection
        if (sourceAccount.getBalancePaise() < request.getAmountPaise()) {
            recordRejectedTransaction(
                    "TRANSFER",
                    sourceAccount,
                    destinationAccount,
                    request.getAmountPaise(),
                    "Insufficient funds. Available: " + sourceAccount.getBalancePaise() + " paise, Required: " + request.getAmountPaise() + " paise",
                    principal,
                    key
            );
            auditService.recordEvent(
                    "TRANSFER_REJECTED",
                    principal.getId(),
                    principal.getUsername(),
                    sourceAccount.getId(),
                    null,
                    "FAILURE",
                    "Transfer rejected: Insufficient funds from " + sourceAccount.getAccountNumber() + " to " + destinationAccount.getAccountNumber(),
                    null
            );
            throw new InsufficientFundsException("Insufficient funds. Available: " + sourceAccount.getBalancePaise() + " paise, Required: " + request.getAmountPaise() + " paise");
        }

        // Apply balance updates
        sourceAccount.setBalancePaise(sourceAccount.getBalancePaise() - request.getAmountPaise());
        destinationAccount.setBalancePaise(destinationAccount.getBalancePaise() + request.getAmountPaise());
        sourceAccount.setUpdatedAt(Instant.now());
        destinationAccount.setUpdatedAt(Instant.now());

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        Transaction txn = new Transaction();
        txn.setTransactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        txn.setIdempotencyKey(key);
        txn.setType("TRANSFER");
        txn.setStatus("SUCCESS");
        txn.setAmountPaise(request.getAmountPaise());
        txn.setCurrency("INR");
        txn.setSourceAccount(sourceAccount);
        txn.setDestinationAccount(destinationAccount);
        txn.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription() : "Internal Fund Transfer");
        txn.setInitiatedByUser(userRepository.getReferenceById(principal.getId()));
        Transaction savedTxn = transactionRepository.save(txn);

        // Double-entry balancing entries
        LedgerEntry debitEntry = new LedgerEntry(savedTxn, sourceAccount, "DEBIT", request.getAmountPaise(), sourceAccount.getBalancePaise());
        LedgerEntry creditEntry = new LedgerEntry(savedTxn, destinationAccount, "CREDIT", request.getAmountPaise(), destinationAccount.getBalancePaise());

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        TransactionResponse response = new TransactionResponse(savedTxn);
        response.setLedgerEntries(List.of(new LedgerEntryDto(debitEntry), new LedgerEntryDto(creditEntry)));

        saveIdempotency(key, payloadHash, principal.getId(), response);
        auditService.recordEvent(
                "TRANSFER_SUCCESS",
                principal.getId(),
                principal.getUsername(),
                sourceAccount.getId(),
                savedTxn.getId(),
                "SUCCESS",
                "Transferred " + request.getAmountPaise() + " paise from " + sourceAccount.getAccountNumber() + " to " + destinationAccount.getAccountNumber(),
                null
        );

        return response;
    }

    // ==========================================
    // 4. TRANSACTION & LEDGER INQUIRIES
    // ==========================================
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsForPrincipal(UserPrincipal principal) {
        boolean isStaff = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR") || a.getAuthority().equals("ROLE_AUDITOR"));

        List<Transaction> txns;
        if (isStaff) {
            txns = transactionRepository.findAllByOrderByCreatedAtDesc();
        } else {
            txns = transactionRepository.findAllByUserId(principal.getId());
        }

        return txns.stream().map(txn -> {
            TransactionResponse resp = new TransactionResponse(txn);
            List<LedgerEntryDto> entries = ledgerEntryRepository.findAllByTransactionId(txn.getId())
                    .stream().map(LedgerEntryDto::new).collect(Collectors.toList());
            resp.setLedgerEntries(entries);
            return resp;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryDto> getAllLedgerEntries() {
        return ledgerEntryRepository.findAllOrderByCreatedAtDesc()
                .stream().map(LedgerEntryDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean verifyGlobalDoubleEntryInvariant() {
        Long totalDebits = ledgerEntryRepository.sumAmountByEntryType("DEBIT");
        Long totalCredits = ledgerEntryRepository.sumAmountByEntryType("CREDIT");
        log.info("Global Ledger Invariant Check: Total Debits = {} paise, Total Credits = {} paise", totalDebits, totalCredits);
        return Objects.equals(totalDebits, totalCredits);
    }

    // ==========================================
    // HELPER METHODS: Idempotency & Hashing
    // ==========================================
    private String resolveIdempotencyKey(String bodyKey, String headerKey) {
        if (StringUtils.hasText(headerKey)) {
            return headerKey.trim();
        }
        if (StringUtils.hasText(bodyKey)) {
            return bodyKey.trim();
        }
        return null;
    }

    private TransactionResponse checkIdempotency(String idempotencyKey, String currentRequestHash) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }

        Optional<IdempotencyRecord> existing = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(currentRequestHash)) {
                throw new IdempotencyConflictException("Idempotency key '" + idempotencyKey + "' has already been used with different request parameters");
            }
            try {
                return objectMapper.readValue(record.getResponseBody(), TransactionResponse.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cached idempotency response", e);
            }
        }
        return null;
    }

    private void saveIdempotency(String idempotencyKey, String requestHash, Long userId, TransactionResponse response) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, requestHash, userId, 200, json);
            idempotencyRecordRepository.save(record);
        } catch (Exception ex) {
            log.error("Failed to save idempotency record for key {}", idempotencyKey, ex);
        }
    }

    private void recordRejectedTransaction(String type, Account source, Account dest, Long amountPaise,
                                           String reason, UserPrincipal principal, String idempotencyKey) {
        try {
            Transaction txn = new Transaction();
            txn.setTransactionRef("REJ-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            txn.setIdempotencyKey(idempotencyKey);
            txn.setType(type);
            txn.setStatus("REJECTED");
            txn.setAmountPaise(amountPaise);
            txn.setCurrency("INR");
            txn.setSourceAccount(source);
            txn.setDestinationAccount(dest);
            txn.setRejectionReason(reason);
            txn.setInitiatedByUser(userRepository.getReferenceById(principal.getId()));
            transactionRepository.save(txn);
        } catch (Exception e) {
            log.error("Failed to record rejected transaction", e);
        }
    }

    private String calculateSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
