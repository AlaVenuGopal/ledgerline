package com.snovasys.ledgerline.dto;

import com.snovasys.ledgerline.entity.Transaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionResponse {
    private UUID id;
    private String transactionRef;
    private String idempotencyKey;
    private String type;
    private String status;
    private Long amountPaise;
    private BigDecimal amountRupees;
    private String currency;
    private Long sourceAccountId;
    private String sourceAccountNumber;
    private Long destinationAccountId;
    private String destinationAccountNumber;
    private String description;
    private String rejectionReason;
    private Instant createdAt;
    private List<LedgerEntryDto> ledgerEntries = new ArrayList<>();

    public TransactionResponse() {}

    public TransactionResponse(Transaction txn) {
        this.id = txn.getId();
        this.transactionRef = txn.getTransactionRef();
        this.idempotencyKey = txn.getIdempotencyKey();
        this.type = txn.getType();
        this.status = txn.getStatus();
        this.amountPaise = txn.getAmountPaise();
        this.amountRupees = BigDecimal.valueOf(txn.getAmountPaise())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
        this.currency = txn.getCurrency();
        if (txn.getSourceAccount() != null) {
            this.sourceAccountId = txn.getSourceAccount().getId();
            this.sourceAccountNumber = txn.getSourceAccount().getAccountNumber();
        }
        if (txn.getDestinationAccount() != null) {
            this.destinationAccountId = txn.getDestinationAccount().getId();
            this.destinationAccountNumber = txn.getDestinationAccount().getAccountNumber();
        }
        this.description = txn.getDescription();
        this.rejectionReason = txn.getRejectionReason();
        this.createdAt = txn.getCreatedAt();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Long amountPaise) { this.amountPaise = amountPaise; }

    public BigDecimal getAmountRupees() { return amountRupees; }
    public void setAmountRupees(BigDecimal amountRupees) { this.amountRupees = amountRupees; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(Long sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public Long getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<LedgerEntryDto> getLedgerEntries() { return ledgerEntries; }
    public void setLedgerEntries(List<LedgerEntryDto> ledgerEntries) { this.ledgerEntries = ledgerEntries; }
}
