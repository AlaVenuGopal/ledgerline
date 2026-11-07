package com.snovasys.ledgerline.service;

import com.snovasys.ledgerline.dto.AuditEventDto;
import com.snovasys.ledgerline.entity.AuditEvent;
import com.snovasys.ledgerline.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent recordEvent(String eventType, Long actorId, String actorUsername, Long targetAccountId,
                                  UUID targetTransactionId, String status, String details, String ipAddress) {
        AuditEvent event = new AuditEvent(eventType, actorId, actorUsername, targetAccountId,
                targetTransactionId, status, details, ipAddress);
        return auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getAllEvents() {
        return auditEventRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AuditEventDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getEventsByActor(Long actorId) {
        return auditEventRepository.findAllByActorIdOrderByCreatedAtDesc(actorId)
                .stream()
                .map(AuditEventDto::new)
                .collect(Collectors.toList());
    }
}
