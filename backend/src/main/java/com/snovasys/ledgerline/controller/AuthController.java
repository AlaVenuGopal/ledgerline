package com.snovasys.ledgerline.controller;

import com.snovasys.ledgerline.dto.AuthResponse;
import com.snovasys.ledgerline.dto.LoginRequest;
import com.snovasys.ledgerline.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        AuthResponse response = authService.authenticate(loginRequest, ipAddress);
        return ResponseEntity.ok(response);
    }
}
