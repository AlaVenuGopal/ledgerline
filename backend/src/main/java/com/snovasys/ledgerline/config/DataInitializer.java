package com.snovasys.ledgerline.config;

import com.snovasys.ledgerline.entity.Account;
import com.snovasys.ledgerline.entity.LedgerEntry;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.entity.Transaction;
import com.snovasys.ledgerline.entity.User;
import com.snovasys.ledgerline.repository.AccountRepository;
import com.snovasys.ledgerline.repository.LedgerEntryRepository;
import com.snovasys.ledgerline.repository.TransactionRepository;
import com.snovasys.ledgerline.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, AccountRepository accountRepository,
                           TransactionRepository transactionRepository, LedgerEntryRepository ledgerEntryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already initialized. Skipping demo data seeding.");
            return;
        }

        log.info("Seeding initial demo users and accounts with balanced double-entry ledger...");

        // 1. Seed Demo Users
        User admin = userRepository.save(new User("admin", "admin@ledgerline.internal",
                passwordEncoder.encode("AdminPassword@123"), "System Administrator", Role.ROLE_ADMINISTRATOR));

        User alice = userRepository.save(new User("alice", "alice@example.com",
                passwordEncoder.encode("AlicePassword@123"), "Alice Sharma", Role.ROLE_ACCOUNT_HOLDER));

        User bob = userRepository.save(new User("bob", "bob@example.com",
                passwordEncoder.encode("BobPassword@123"), "Bob Verma", Role.ROLE_ACCOUNT_HOLDER));

        User auditor = userRepository.save(new User("auditor", "auditor@regulatory.internal",
                passwordEncoder.encode("AuditorPassword@123"), "Compliance Auditor", Role.ROLE_AUDITOR));

        // 2. Fetch System Treasury Account
        Account treasury = accountRepository.findSystemTreasuryAccount()
                .orElseGet(() -> accountRepository.save(new Account("LL-SYS-TREASURY-01", null, "SYSTEM_TREASURY", "INR", 100000000000L)));

        // 3. Seed Alice's Account (Initial balance: ₹50,000 = 5,000,000 paise)
        Long aliceInitialPaise = 5000000L;
        Account aliceAccount = accountRepository.save(new Account("LL-ACC-ALICE-01", alice, "CUSTOMER", "INR", aliceInitialPaise));

        treasury.setBalancePaise(treasury.getBalancePaise() - aliceInitialPaise);
        accountRepository.save(treasury);

        Transaction aliceFundingTxn = new Transaction();
        aliceFundingTxn.setTransactionRef("SEED-INIT-ALICE");
        aliceFundingTxn.setType("DEPOSIT");
        aliceFundingTxn.setStatus("SUCCESS");
        aliceFundingTxn.setAmountPaise(aliceInitialPaise);
        aliceFundingTxn.setCurrency("INR");
        aliceFundingTxn.setSourceAccount(treasury);
        aliceFundingTxn.setDestinationAccount(aliceAccount);
        aliceFundingTxn.setDescription("Initial Account Opening Deposit");
        aliceFundingTxn.setInitiatedByUser(admin);
        Transaction savedAliceTxn = transactionRepository.save(aliceFundingTxn);

        ledgerEntryRepository.save(new LedgerEntry(savedAliceTxn, treasury, "DEBIT", aliceInitialPaise, treasury.getBalancePaise()));
        ledgerEntryRepository.save(new LedgerEntry(savedAliceTxn, aliceAccount, "CREDIT", aliceInitialPaise, aliceAccount.getBalancePaise()));

        // 4. Seed Bob's Account (Initial balance: ₹25,000 = 2,500,000 paise)
        Long bobInitialPaise = 2500000L;
        Account bobAccount = accountRepository.save(new Account("LL-ACC-BOB-01", bob, "CUSTOMER", "INR", bobInitialPaise));

        treasury.setBalancePaise(treasury.getBalancePaise() - bobInitialPaise);
        accountRepository.save(treasury);

        Transaction bobFundingTxn = new Transaction();
        bobFundingTxn.setTransactionRef("SEED-INIT-BOB");
        bobFundingTxn.setType("DEPOSIT");
        bobFundingTxn.setStatus("SUCCESS");
        bobFundingTxn.setAmountPaise(bobInitialPaise);
        bobFundingTxn.setCurrency("INR");
        bobFundingTxn.setSourceAccount(treasury);
        bobFundingTxn.setDestinationAccount(bobAccount);
        bobFundingTxn.setDescription("Initial Account Opening Deposit");
        bobFundingTxn.setInitiatedByUser(admin);
        Transaction savedBobTxn = transactionRepository.save(bobFundingTxn);

        ledgerEntryRepository.save(new LedgerEntry(savedBobTxn, treasury, "DEBIT", bobInitialPaise, treasury.getBalancePaise()));
        ledgerEntryRepository.save(new LedgerEntry(savedBobTxn, bobAccount, "CREDIT", bobInitialPaise, bobAccount.getBalancePaise()));

        log.info("Data seeding complete. Initial accounts created with balanced ledger.");
    }
}
