package com.snovasys.ledgerline.service;

import com.snovasys.ledgerline.dto.AuthResponse;
import com.snovasys.ledgerline.dto.LoginRequest;
import com.snovasys.ledgerline.security.JwtTokenProvider;
import com.snovasys.ledgerline.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    public AuthResponse authenticate(LoginRequest loginRequest, String ipAddress) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String role = userPrincipal.getAuthorities().iterator().next().getAuthority();

            auditService.recordEvent(
                    "AUTH_LOGIN_SUCCESS",
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    null,
                    null,
                    "SUCCESS",
                    "User logged in successfully",
                    ipAddress
            );

            return new AuthResponse(
                    jwt,
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    userPrincipal.getEmail(),
                    userPrincipal.getFullName(),
                    role
            );
        } catch (Exception ex) {
            auditService.recordEvent(
                    "AUTH_LOGIN_FAILURE",
                    null,
                    loginRequest.getUsername(),
                    null,
                    null,
                    "FAILURE",
                    "Failed login attempt: " + ex.getMessage(),
                    ipAddress
            );
            throw ex;
        }
    }
}
