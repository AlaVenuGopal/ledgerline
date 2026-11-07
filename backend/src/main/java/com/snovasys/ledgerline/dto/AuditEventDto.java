package com.snovasys.ledgerline.dto;

import com.snovasys.ledgerline.entity.AuditEvent;
import java.time.Instant;
import java.util.UUID;

public class AuditEventDto {
    private UUID id;
    private String eventType;
    private Long actorId;
    private String actorUsername;
    private Long targetAccountId;
    private UUID targetTransactionId;
    private String status;
    private String details;
    private String ipAddress;
    private Instant createdAt;

    public AuditEventDto() {}

    public AuditEventDto(AuditEvent event) {
        this.id = event.getId();
        this.eventType = event.getEventType();
        this.actorId = event.getActorId();
        this.actorUsername = event.getActorUsername();
        this.targetAccountId = event.getTargetAccountId();
        this.targetTransactionId = event.getTargetTransactionId();
        this.status = event.getStatus();
        this.details = event.getDetails();
        this.ipAddress = event.getIpAddress();
        this.createdAt = event.getCreatedAt();
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
