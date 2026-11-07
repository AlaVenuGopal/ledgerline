package com.snovasys.ledgerline.exception;

import com.snovasys.ledgerline.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest request) {
        log.warn("Insufficient funds rejection: {}", ex.getMessage());
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "INSUFFICIENT_FUNDS", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest request) {
        log.warn("Account not found: {}", ex.getMessage());
        ApiError error = new ApiError(HttpStatus.NOT_FOUND.value(), "ACCOUNT_NOT_FOUND", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(UnauthorizedAccountAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorizedAccountAccess(UnauthorizedAccountAccessException ex, HttpServletRequest request) {
        log.warn("Unauthorized account access attempt: {}", ex.getMessage());
        ApiError error = new ApiError(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleIdempotencyConflict(IdempotencyConflictException ex, HttpServletRequest request) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        ApiError error = new ApiError(HttpStatus.CONFLICT.value(), "IDEMPOTENCY_CONFLICT", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ApiError> handleInvalidAmount(InvalidAmountException ex, HttpServletRequest request) {
        log.warn("Invalid amount: {}", ex.getMessage());
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "INVALID_AMOUNT", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED", "Input validation failed", request.getRequestURI(), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiError error = new ApiError(HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED", "You do not have permission to access this resource", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials authentication failure: {}", ex.getMessage());
        ApiError error = new ApiError(HttpStatus.UNAUTHORIZED.value(), "INVALID_CREDENTIALS", "Invalid username or password", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled server exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiError error = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please contact support.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
