package com.snovasys.ledgerline.dto;

import com.snovasys.ledgerline.entity.LedgerEntry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public class LedgerEntryDto {
    private UUID id;
    private UUID transactionId;
    private Long accountId;
    private String accountNumber;
    private String entryType; // DEBIT or CREDIT
    private Long amountPaise;
    private BigDecimal amountRupees;
    private Long balanceAfterPaise;
    private BigDecimal balanceAfterRupees;
    private Instant createdAt;

    public LedgerEntryDto() {}

    public LedgerEntryDto(LedgerEntry entry) {
        this.id = entry.getId();
        this.transactionId = entry.getTransaction().getId();
        this.accountId = entry.getAccount().getId();
        this.accountNumber = entry.getAccount().getAccountNumber();
        this.entryType = entry.getEntryType();
        this.amountPaise = entry.getAmountPaise();
        this.amountRupees = BigDecimal.valueOf(entry.getAmountPaise())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
        this.balanceAfterPaise = entry.getBalanceAfterPaise();
        this.balanceAfterRupees = BigDecimal.valueOf(entry.getBalanceAfterPaise())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
        this.createdAt = entry.getCreatedAt();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public Long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Long amountPaise) { this.amountPaise = amountPaise; }

    public BigDecimal getAmountRupees() { return amountRupees; }
    public void setAmountRupees(BigDecimal amountRupees) { this.amountRupees = amountRupees; }

    public Long getBalanceAfterPaise() { return balanceAfterPaise; }
    public void setBalanceAfterPaise(Long balanceAfterPaise) { this.balanceAfterPaise = balanceAfterPaise; }

    public BigDecimal getBalanceAfterRupees() { return balanceAfterRupees; }
    public void setBalanceAfterRupees(BigDecimal balanceAfterRupees) { this.balanceAfterRupees = balanceAfterRupees; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
