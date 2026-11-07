package com.snovasys.ledgerline.exception;

public class IdempotencyConflictException extends LedgerlineException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
