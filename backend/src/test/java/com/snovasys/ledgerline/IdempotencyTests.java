package com.snovasys.ledgerline;

import com.snovasys.ledgerline.dto.TransactionResponse;
import com.snovasys.ledgerline.dto.TransferRequest;
import com.snovasys.ledgerline.entity.Account;
import com.snovasys.ledgerline.exception.IdempotencyConflictException;
import com.snovasys.ledgerline.repository.AccountRepository;
import com.snovasys.ledgerline.security.CustomUserDetailsService;
import com.snovasys.ledgerline.security.UserPrincipal;
import com.snovasys.ledgerline.service.FinancialEngineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class IdempotencyTests {

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
    @DisplayName("Duplicate request with same idempotency key returns original response without second transfer")
    void duplicateIdempotencyKeyReplaysResponseWithoutRetransfer() {
        UserPrincipal alice = getPrincipal("alice");
        Account aliceAcc = accountRepository.findByAccountNumber("LL-ACC-ALICE-01").orElseThrow();
        Account bobAcc = accountRepository.findByAccountNumber("LL-ACC-BOB-01").orElseThrow();

        String idempotencyKey = "IDEM-" + UUID.randomUUID();
        Long transferAmount = 10000L; // ₹100.00

        Long aliceBefore = accountRepository.findById(aliceAcc.getId()).orElseThrow().getBalancePaise();
        Long bobBefore = accountRepository.findById(bobAcc.getId()).orElseThrow().getBalancePaise();

        TransferRequest request = new TransferRequest(aliceAcc.getId(), bobAcc.getId(), transferAmount, "Idempotent payment", idempotencyKey);

        // First attempt
        TransactionResponse firstResponse = financialEngineService.transfer(request, alice, idempotencyKey);
        assertThat(firstResponse.getStatus()).isEqualTo("SUCCESS");

        Long aliceAfterFirst = accountRepository.findById(aliceAcc.getId()).orElseThrow().getBalancePaise();
        Long bobAfterFirst = accountRepository.findById(bobAcc.getId()).orElseThrow().getBalancePaise();

        assertThat(aliceAfterFirst).isEqualTo(aliceBefore - transferAmount);
        assertThat(bobAfterFirst).isEqualTo(bobBefore + transferAmount);

        // Second attempt with exact same key and payload
        TransactionResponse secondResponse = financialEngineService.transfer(request, alice, idempotencyKey);
        assertThat(secondResponse.getTransactionRef()).isEqualTo(firstResponse.getTransactionRef());

        // Crucial: Balances must NOT have changed again!
        Long aliceAfterSecond = accountRepository.findById(aliceAcc.getId()).orElseThrow().getBalancePaise();
        Long bobAfterSecond = accountRepository.findById(bobAcc.getId()).orElseThrow().getBalancePaise();

        assertThat(aliceAfterSecond).isEqualTo(aliceAfterFirst);
        assertThat(bobAfterSecond).isEqualTo(bobAfterFirst);
    }

    @Test
    @DisplayName("Idempotency key reused with different payload is rejected with IdempotencyConflictException")
    void keyReusedWithDifferentPayloadThrowsConflict() {
        UserPrincipal alice = getPrincipal("alice");
        Account aliceAcc = accountRepository.findByAccountNumber("LL-ACC-ALICE-01").orElseThrow();
        Account bobAcc = accountRepository.findByAccountNumber("LL-ACC-BOB-01").orElseThrow();

        String idempotencyKey = "IDEM-CONFLICT-" + UUID.randomUUID();

        // 1. First request: ₹100.00
        TransferRequest req1 = new TransferRequest(aliceAcc.getId(), bobAcc.getId(), 10000L, "Initial payload", idempotencyKey);
        financialEngineService.transfer(req1, alice, idempotencyKey);

        // 2. Second request with SAME key but ₹200.00 amount (different payload)
        TransferRequest req2 = new TransferRequest(aliceAcc.getId(), bobAcc.getId(), 20000L, "Changed amount", idempotencyKey);

        assertThrows(IdempotencyConflictException.class, () -> {
            financialEngineService.transfer(req2, alice, idempotencyKey);
        });
    }
}
