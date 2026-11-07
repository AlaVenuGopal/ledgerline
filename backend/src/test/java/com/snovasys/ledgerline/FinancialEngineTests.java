package com.snovasys.ledgerline;

import com.snovasys.ledgerline.dto.DepositRequest;
import com.snovasys.ledgerline.dto.TransactionResponse;
import com.snovasys.ledgerline.dto.TransferRequest;
import com.snovasys.ledgerline.dto.WithdrawRequest;
import com.snovasys.ledgerline.entity.Account;
import com.snovasys.ledgerline.exception.InsufficientFundsException;
import com.snovasys.ledgerline.repository.AccountRepository;
import com.snovasys.ledgerline.security.CustomUserDetailsService;
import com.snovasys.ledgerline.security.UserPrincipal;
import com.snovasys.ledgerline.service.FinancialEngineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class FinancialEngineTests {

    @Autowired
    private FinancialEngineService financialEngineService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private UserPrincipal getPrincipal(String username) {
        return (UserPrincipal) userDetailsService.loadUserByUsername(username);
    }

    @Test
    @DisplayName("Deposit updates balance atomically and maintains double-entry equality")
    void depositCreatesBalancedEntries() {
        UserPrincipal alice = getPrincipal("alice");
        Account account = accountRepository.findByAccountNumber("LL-ACC-ALICE-01").orElseThrow();
        Long balanceBefore = account.getBalancePaise();
        Long depositAmount = 250000L; // ₹2,500.00

        TransactionResponse response = financialEngineService.deposit(
                new DepositRequest(account.getId(), depositAmount, "Bonus Deposit", null),
                alice,
                null
        );

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getLedgerEntries()).hasSize(2);

        Long debits = response.getLedgerEntries().stream()
                .filter(e -> "DEBIT".equals(e.getEntryType()))
                .mapToLong(e -> e.getAmountPaise()).sum();

        Long credits = response.getLedgerEntries().stream()
                .filter(e -> "CREDIT".equals(e.getEntryType()))
                .mapToLong(e -> e.getAmountPaise()).sum();

        assertThat(debits).isEqualTo(credits).isEqualTo(depositAmount);

        Account accountAfter = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(accountAfter.getBalancePaise()).isEqualTo(balanceBefore + depositAmount);
    }

    @Test
    @DisplayName("Withdrawal rejects insufficient funds without modifying balances")
    void withdrawalRejectsOverdraft() {
        UserPrincipal bob = getPrincipal("bob");
        Account account = accountRepository.findByAccountNumber("LL-ACC-BOB-01").orElseThrow();
        Long balanceBefore = account.getBalancePaise();
        Long excessiveAmount = balanceBefore + 1000000L; // Excess of available funds

        assertThrows(InsufficientFundsException.class, () -> {
            financialEngineService.withdraw(
                    new WithdrawRequest(account.getId(), excessiveAmount, "Overdraft attempt", null),
                    bob,
                    null
            );
        });

        Account accountAfter = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(accountAfter.getBalancePaise()).isEqualTo(balanceBefore);
    }

    @Test
    @DisplayName("Internal transfer transfers exact amount atomically and conserves money")
    void internalTransferExecutesAtomically() {
        UserPrincipal alice = getPrincipal("alice");
        Account aliceAcc = accountRepository.findByAccountNumber("LL-ACC-ALICE-01").orElseThrow();
        Account bobAcc = accountRepository.findByAccountNumber("LL-ACC-BOB-01").orElseThrow();

        Long aliceBefore = aliceAcc.getBalancePaise();
        Long bobBefore = bobAcc.getBalancePaise();
        Long transferAmount = 150000L; // ₹1,500.00

        TransactionResponse response = financialEngineService.transfer(
                new TransferRequest(aliceAcc.getId(), bobAcc.getId(), transferAmount, "Dinner split", null),
                alice,
                null
        );

        assertThat(response.getStatus()).isEqualTo("SUCCESS");

        Account aliceAfter = accountRepository.findById(aliceAcc.getId()).orElseThrow();
        Account bobAfter = accountRepository.findById(bobAcc.getId()).orElseThrow();

        assertThat(aliceAfter.getBalancePaise()).isEqualTo(aliceBefore - transferAmount);
        assertThat(bobAfter.getBalancePaise()).isEqualTo(bobBefore + transferAmount);

        // Global double-entry invariant verification
        boolean isBalanced = financialEngineService.verifyGlobalDoubleEntryInvariant();
        assertThat(isBalanced).isTrue();
    }
}
