package com.snovasys.ledgerline.controller;

import com.snovasys.ledgerline.dto.DepositRequest;
import com.snovasys.ledgerline.dto.TransactionResponse;
import com.snovasys.ledgerline.dto.TransferRequest;
import com.snovasys.ledgerline.dto.WithdrawRequest;
import com.snovasys.ledgerline.security.UserPrincipal;
import com.snovasys.ledgerline.service.FinancialEngineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final FinancialEngineService financialEngineService;

    public TransactionController(FinancialEngineService financialEngineService) {
        this.financialEngineService = financialEngineService;
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('ACCOUNT_HOLDER', 'ADMINISTRATOR')")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal UserPrincipal principal) {
        TransactionResponse response = financialEngineService.deposit(request, principal, idempotencyKeyHeader);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyRole('ACCOUNT_HOLDER', 'ADMINISTRATOR')")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal UserPrincipal principal) {
        TransactionResponse response = financialEngineService.withdraw(request, principal, idempotencyKeyHeader);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ACCOUNT_HOLDER', 'ADMINISTRATOR')")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal UserPrincipal principal) {
        TransactionResponse response = financialEngineService.transfer(request, principal, idempotencyKeyHeader);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(financialEngineService.getTransactionsForPrincipal(principal));
    }
}
