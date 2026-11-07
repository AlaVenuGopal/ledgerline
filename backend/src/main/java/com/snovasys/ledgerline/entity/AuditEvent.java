package com.snovasys.ledgerline.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    @Column(name = "target_account_id")
    private Long targetAccountId;

    @Column(name = "target_transaction_id")
    private UUID targetTransactionId;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAILURE

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AuditEvent() {}

    public AuditEvent(String eventType, Long actorId, String actorUsername, Long targetAccountId,
                      UUID targetTransactionId, String status, String details, String ipAddress) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.targetAccountId = targetAccountId;
        this.targetTransactionId = targetTransactionId;
        this.status = status;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public Long getTargetAccountId() { return targetAccountId; }
    public void setTargetAccountId(Long targetAccountId) { this.targetAccountId = targetAccountId; }

    public UUID getTargetTransactionId() { return targetTransactionId; }
    public void setTargetTransactionId(UUID targetTransactionId) { this.targetTransactionId = targetTransactionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
