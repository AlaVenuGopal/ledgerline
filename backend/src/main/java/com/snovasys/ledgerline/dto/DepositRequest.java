package com.snovasys.ledgerline.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DepositRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1 paisa")
    private Long amountPaise;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    private String idempotencyKey;

    public DepositRequest() {}

    public DepositRequest(Long accountId, Long amountPaise, String description, String idempotencyKey) {
        this.accountId = accountId;
        this.amountPaise = amountPaise;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Long amountPaise) { this.amountPaise = amountPaise; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
