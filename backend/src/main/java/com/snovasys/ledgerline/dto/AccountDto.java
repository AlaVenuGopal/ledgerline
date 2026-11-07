package com.snovasys.ledgerline.dto;

import com.snovasys.ledgerline.entity.Account;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class AccountDto {
    private Long id;
    private String accountNumber;
    private Long userId;
    private String ownerUsername;
    private String ownerFullName;
    private String accountType;
    private String currency;
    private Long balancePaise;
    private BigDecimal balanceRupees;
    private String status;
    private Instant createdAt;

    public AccountDto() {}

    public AccountDto(Account account) {
        this.id = account.getId();
        this.accountNumber = account.getAccountNumber();
        if (account.getUser() != null) {
            this.userId = account.getUser().getId();
            this.ownerUsername = account.getUser().getUsername();
            this.ownerFullName = account.getUser().getFullName();
        }
        this.accountType = account.getAccountType();
        this.currency = account.getCurrency();
        this.balancePaise = account.getBalancePaise();
        this.balanceRupees = BigDecimal.valueOf(account.getBalancePaise())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
        this.status = account.getStatus();
        this.createdAt = account.getCreatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getOwnerFullName() { return ownerFullName; }
    public void setOwnerFullName(String ownerFullName) { this.ownerFullName = ownerFullName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getBalancePaise() { return balancePaise; }
    public void setBalancePaise(Long balancePaise) { this.balancePaise = balancePaise; }

    public BigDecimal getBalanceRupees() { return balanceRupees; }
    public void setBalanceRupees(BigDecimal balanceRupees) { this.balanceRupees = balanceRupees; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
