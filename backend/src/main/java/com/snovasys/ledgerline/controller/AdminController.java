package com.snovasys.ledgerline.controller;

import com.snovasys.ledgerline.dto.AccountDto;
import com.snovasys.ledgerline.dto.CreateAccountRequest;
import com.snovasys.ledgerline.dto.CreateUserRequest;
import com.snovasys.ledgerline.dto.UserDto;
import com.snovasys.ledgerline.entity.User;
import com.snovasys.ledgerline.exception.LedgerlineException;
import com.snovasys.ledgerline.repository.UserRepository;
import com.snovasys.ledgerline.security.UserPrincipal;
import com.snovasys.ledgerline.service.AccountService;
import com.snovasys.ledgerline.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminController {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminController(UserRepository userRepository, AccountService accountService,
                           PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new LedgerlineException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new LedgerlineException("Email already exists: " + request.getEmail());
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getRole()
        );
        User saved = userRepository.save(user);

        auditService.recordEvent(
                "USER_CREATED",
                principal.getId(),
                principal.getUsername(),
                null,
                null,
                "SUCCESS",
                "Created user " + saved.getUsername() + " with role " + saved.getRole(),
                null
        );

        return ResponseEntity.ok(new UserDto(saved));
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody CreateAccountRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        AccountDto account = accountService.createAccount(request, principal);
        return ResponseEntity.ok(account);
    }
}
