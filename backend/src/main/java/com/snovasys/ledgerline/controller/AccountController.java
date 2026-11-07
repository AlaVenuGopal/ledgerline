package com.snovasys.ledgerline.controller;

import com.snovasys.ledgerline.dto.AccountDto;
import com.snovasys.ledgerline.dto.AccountStatementResponse;
import com.snovasys.ledgerline.security.UserPrincipal;
import com.snovasys.ledgerline.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getMyAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.getAccountsForPrincipal(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.getAccountById(id, principal));
    }

    @GetMapping("/lookup/{accountNumber}")
    public ResponseEntity<AccountDto> lookupAccount(@PathVariable String accountNumber, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber, principal));
    }

    @GetMapping("/{id}/statement")
    public ResponseEntity<AccountStatementResponse> getStatement(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.getAccountStatement(id, principal));
    }
}
