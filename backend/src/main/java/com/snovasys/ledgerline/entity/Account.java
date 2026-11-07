package com.snovasys.ledgerline.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 30)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType = "CUSTOMER";

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "balance_paise", nullable = false)
    private Long balancePaise = 0L;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Account() {}

    public Account(String accountNumber, User user, String accountType, String currency, Long initialBalancePaise) {
        this.accountNumber = accountNumber;
        this.user = user;
        this.accountType = accountType;
        this.currency = currency;
        this.balancePaise = initialBalancePaise;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getBalancePaise() { return balancePaise; }
    public void setBalancePaise(Long balancePaise) { this.balancePaise = balancePaise; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
