package com.snovasys.ledgerline.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateAccountRequest {

    @NotNull(message = "Owner User ID is required")
    private Long userId;

    @Min(value = 0, message = "Initial deposit cannot be negative")
    private Long initialDepositPaise = 0L;

    private String currency = "INR";

    public CreateAccountRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getInitialDepositPaise() { return initialDepositPaise; }
    public void setInitialDepositPaise(Long initialDepositPaise) { this.initialDepositPaise = initialDepositPaise; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
