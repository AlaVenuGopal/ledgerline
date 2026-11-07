package com.snovasys.ledgerline.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 40)
    private String transactionRef;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, length = 20)
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAILED, REJECTED

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(length = 255)
    private String description;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by_user_id")
    private User initiatedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Transaction() {}

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

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Account getSourceAccount() { return sourceAccount; }
    public void setSourceAccount(Account sourceAccount) { this.sourceAccount = sourceAccount; }

    public Account getDestinationAccount() { return destinationAccount; }
    public void setDestinationAccount(Account destinationAccount) { this.destinationAccount = destinationAccount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public User getInitiatedByUser() { return initiatedByUser; }
    public void setInitiatedByUser(User initiatedByUser) { this.initiatedByUser = initiatedByUser; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
