package com.snovasys.ledgerline.exception;

public class LedgerlineException extends RuntimeException {
    public LedgerlineException(String message) {
        super(message);
    }

    public LedgerlineException(String message, Throwable cause) {
        super(message, cause);
    }
}
