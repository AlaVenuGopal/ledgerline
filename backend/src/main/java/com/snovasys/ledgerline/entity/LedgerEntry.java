package com.snovasys.ledgerline.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType; // DEBIT or CREDIT

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(name = "balance_after_paise", nullable = false)
    private Long balanceAfterPaise;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public LedgerEntry() {}

    public LedgerEntry(Transaction transaction, Account account, String entryType, Long amountPaise, Long balanceAfterPaise) {
        this.id = UUID.randomUUID();
        this.transaction = transaction;
        this.account = account;
        this.entryType = entryType;
        this.amountPaise = amountPaise;
        this.balanceAfterPaise = balanceAfterPaise;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public Long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Long amountPaise) { this.amountPaise = amountPaise; }

    public Long getBalanceAfterPaise() { return balanceAfterPaise; }
    public void setBalanceAfterPaise(Long balanceAfterPaise) { this.balanceAfterPaise = balanceAfterPaise; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
