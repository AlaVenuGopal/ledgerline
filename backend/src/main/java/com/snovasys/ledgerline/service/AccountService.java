package com.snovasys.ledgerline.service;

import com.snovasys.ledgerline.dto.AccountDto;
import com.snovasys.ledgerline.dto.AccountStatementResponse;
import com.snovasys.ledgerline.dto.CreateAccountRequest;
import com.snovasys.ledgerline.dto.LedgerEntryDto;
import com.snovasys.ledgerline.entity.Account;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.entity.User;
import com.snovasys.ledgerline.exception.AccountNotFoundException;
import com.snovasys.ledgerline.exception.UnauthorizedAccountAccessException;
import com.snovasys.ledgerline.repository.AccountRepository;
import com.snovasys.ledgerline.repository.LedgerEntryRepository;
import com.snovasys.ledgerline.repository.UserRepository;
import com.snovasys.ledgerline.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditService auditService;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository,
                          LedgerEntryRepository ledgerEntryRepository, AuditService auditService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsForPrincipal(UserPrincipal principal) {
        boolean isStaff = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMINISTRATOR.name()) ||
                               a.getAuthority().equals(Role.ROLE_AUDITOR.name()));

        if (isStaff) {
            return accountRepository.findAll().stream()
                    .map(AccountDto::new)
                    .collect(Collectors.toList());
        }

        return accountRepository.findAllByUserId(principal.getId()).stream()
                .map(AccountDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountById(Long accountId, UserPrincipal principal) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        validateOwnership(account, principal);
        return new AccountDto(account);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountByNumber(String accountNumber, UserPrincipal principal) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));

        // For public transfers inquiry or staff, allow view; if account holder viewing details, check ownership
        return new AccountDto(account);
    }

    @Transactional
    public AccountDto createAccount(CreateAccountRequest request, UserPrincipal principal) {
        User owner = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AccountNotFoundException("User not found with ID: " + request.getUserId()));

        String accountNumber = "LL-ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Account account = new Account(accountNumber, owner, "CUSTOMER", request.getCurrency(), 0L);
        Account savedAccount = accountRepository.save(account);

        auditService.recordEvent(
                "ACCOUNT_CREATED",
                principal.getId(),
                principal.getUsername(),
                savedAccount.getId(),
                null,
                "SUCCESS",
                "Created account " + accountNumber + " for user " + owner.getUsername(),
                null
        );

        return new AccountDto(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse getAccountStatement(Long accountId, UserPrincipal principal) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        validateOwnership(account, principal);

        List<LedgerEntryDto> entries = ledgerEntryRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(LedgerEntryDto::new)
                .collect(Collectors.toList());

        return new AccountStatementResponse(new AccountDto(account), entries);
    }

    public void validateOwnership(Account account, UserPrincipal principal) {
        boolean isStaff = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMINISTRATOR.name()) ||
                               a.getAuthority().equals(Role.ROLE_AUDITOR.name()));

        if (isStaff) {
            return;
        }

        if (account.getUser() == null || !account.getUser().getId().equals(principal.getId())) {
            throw new UnauthorizedAccountAccessException("You do not have permission to access account: " + account.getAccountNumber());
        }
    }
}
