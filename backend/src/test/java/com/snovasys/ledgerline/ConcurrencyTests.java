package com.snovasys.ledgerline;

import com.snovasys.ledgerline.dto.CreateAccountRequest;
import com.snovasys.ledgerline.dto.DepositRequest;
import com.snovasys.ledgerline.dto.TransferRequest;
import com.snovasys.ledgerline.dto.WithdrawRequest;
import com.snovasys.ledgerline.entity.Account;
import com.snovasys.ledgerline.exception.InsufficientFundsException;
import com.snovasys.ledgerline.repository.AccountRepository;
import com.snovasys.ledgerline.security.CustomUserDetailsService;
import com.snovasys.ledgerline.security.UserPrincipal;
import com.snovasys.ledgerline.service.AccountService;
import com.snovasys.ledgerline.service.FinancialEngineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ConcurrencyTests {

    @Autowired
    private FinancialEngineService financialEngineService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private UserPrincipal getPrincipal(String username) {
        return (UserPrincipal) userDetailsService.loadUserByUsername(username);
    }

    @Test
    @DisplayName("Concurrent withdrawals never cause negative balances or overdraw the account")
    void concurrentWithdrawalsNeverOverdraw() throws InterruptedException {
        UserPrincipal alice = getPrincipal("alice");

        // 1. Create a dedicated test account with exactly ₹1,000.00 (100,000 paise)
        var accDto = accountService.createAccount(new CreateAccountRequest() {{
            setUserId(alice.getId());
            setCurrency("INR");
        }}, alice);

        Long initialBalance = 100000L;
        financialEngineService.deposit(new DepositRequest(accDto.getId(), initialBalance, "Fund test account", null), alice, null);

        // 2. Prepare 10 concurrent threads each attempting to withdraw ₹200.00 (20,000 paise)
        // Total attempted = ₹2,000.00 (200,000 paise). Available = only ₹1,000.00 (100,000 paise).
        int threadCount = 10;
        Long withdrawalAmount = 20000L;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await(); // Synchronize all threads to fire simultaneously
                    financialEngineService.withdraw(
                            new WithdrawRequest(accDto.getId(), withdrawalAmount, "Concurrent withdrawal " + index, null),
                            alice,
                            null
                    );
                    successCount.incrementAndGet();
                } catch (InsufficientFundsException ex) {
                    rejectedCount.incrementAndGet();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown(); // Fire!
        boolean finished = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();

        // Exactly 5 withdrawals must succeed (5 * 20,000 = 100,000 paise = entire balance)
        // Exactly 5 withdrawals must be rejected due to insufficient funds
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(rejectedCount.get()).isEqualTo(5);

        // Final balance must be EXACTLY 0, never negative!
        Account finalAccount = accountRepository.findById(accDto.getId()).orElseThrow();
        assertThat(finalAccount.getBalancePaise()).isEqualTo(0L);

        // Verify global double-entry invariant remains 100% balanced
        assertThat(financialEngineService.verifyGlobalDoubleEntryInvariant()).isTrue();
    }

    @Test
    @DisplayName("Bidirectional concurrent transfers do not deadlock due to deterministic lock ordering")
    void bidirectionalTransfersDoNotDeadlock() throws InterruptedException {
        UserPrincipal alice = getPrincipal("alice");
        UserPrincipal bob = getPrincipal("bob");

        Account aliceAcc = accountRepository.findByAccountNumber("LL-ACC-ALICE-01").orElseThrow();
        Account bobAcc = accountRepository.findByAccountNumber("LL-ACC-BOB-01").orElseThrow();

        int iterations = 20;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(iterations * 2);
        AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            // Thread A: Alice -> Bob
            executor.submit(() -> {
                try {
                    financialEngineService.transfer(
                            new TransferRequest(aliceAcc.getId(), bobAcc.getId(), 100L, "A to B", null),
                            alice,
                            null
                    );
                    completedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            // Thread B: Bob -> Alice
            executor.submit(() -> {
                try {
                    financialEngineService.transfer(
                            new TransferRequest(bobAcc.getId(), aliceAcc.getId(), 100L, "B to A", null),
                            bob,
                            null
                    );
                    completedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(completedCount.get()).isEqualTo(iterations * 2);
        assertThat(financialEngineService.verifyGlobalDoubleEntryInvariant()).isTrue();
    }
}
