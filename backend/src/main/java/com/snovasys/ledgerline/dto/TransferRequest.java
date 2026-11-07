package com.snovasys.ledgerline.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TransferRequest {

    @NotNull(message = "Source account ID is required")
    private Long sourceAccountId;

    private Long destinationAccountId;

    private String destinationAccountNumber;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1 paisa")
    private Long amountPaise;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    private String idempotencyKey;

    public TransferRequest() {}

    public TransferRequest(Long sourceAccountId, Long destinationAccountId, Long amountPaise, String description, String idempotencyKey) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amountPaise = amountPaise;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(Long sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public Long getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }

    public Long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Long amountPaise) { this.amountPaise = amountPaise; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
